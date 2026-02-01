'use client';

import React, { useState, useEffect, useRef } from 'react';
import {
  Box,
  Paper,
  Typography,
  IconButton,
  List,
  ListItem,
  ListItemAvatar,
  ListItemText,
  Avatar,
  TextField,
  InputAdornment,
  Badge,
  Divider,
  CircularProgress,
  Fade
} from '@mui/material';
import {
  Close as CloseIcon,
  OpenInFull as OpenInFullIcon,
  Send as SendIcon,
  AttachFile as AttachFileIcon,
  Search as SearchIcon
} from '@mui/icons-material';
import { useMessaging } from '@/hooks/useMessaging';
import { Conversation, Message as MessageType } from '@/types/messaging';
import { formatDistanceToNow } from 'date-fns';

interface MessagingPopupProps {
  onClose: () => void;
  onOpenFull: () => void;
}

export default function MessagingPopup({ onClose, onOpenFull }: MessagingPopupProps) {
  const {
    conversations,
    messages,
    unreadCount,
    typingUsers,
    onlineUsers,
    isConnected,
    loading,
    loadMessages,
    sendMessage,
    sendTyping,
    getCurrentUserId,
    getCurrentUserName
  } = useMessaging();

  const [selectedConversation, setSelectedConversation] = useState<Conversation | null>(null);
  const [messageInput, setMessageInput] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [sending, setSending] = useState(false);
  
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const typingTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  // Auto-scroll to bottom when new messages arrive
  useEffect(() => {
    if (messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages, selectedConversation]);

  // Load messages when conversation is selected
  useEffect(() => {
    if (selectedConversation) {
      loadMessages(selectedConversation.id);
    }
  }, [selectedConversation]);

  const handleSelectConversation = (conversation: Conversation) => {
    setSelectedConversation(conversation);
  };

  const handleBackToList = () => {
    setSelectedConversation(null);
  };

  const handleSendMessage = async () => {
    if (!messageInput.trim() || !selectedConversation || sending) return;

    const content = messageInput.trim();
    setMessageInput('');
    setSending(true);

    try {
      await sendMessage(
        selectedConversation.otherParticipantId,
        content,
        selectedConversation.id
      );
      // Stop typing indicator
      sendTyping(selectedConversation.otherParticipantId, false);
    } catch (error) {
      console.error('Failed to send message:', error);
      setMessageInput(content); // Restore message on error
    } finally {
      setSending(false);
    }
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setMessageInput(e.target.value);

    // Send typing indicator
    if (selectedConversation) {
      sendTyping(selectedConversation.otherParticipantId, true);

      // Clear existing timeout
      if (typingTimeoutRef.current) {
        clearTimeout(typingTimeoutRef.current);
      }

      // Stop typing after 3 seconds of inactivity
      typingTimeoutRef.current = setTimeout(() => {
        sendTyping(selectedConversation.otherParticipantId, false);
      }, 3000);
    }
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
  };

  const filteredConversations = conversations.filter(conv =>
    conv.otherParticipantName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
    conv.lastMessagePreview.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const currentUserId = getCurrentUserId();
  const conversationMessages = selectedConversation ? messages[selectedConversation.id] || [] : [];
  const isOtherUserTyping = selectedConversation ? typingUsers[selectedConversation.otherParticipantId] : false;
  const isOtherUserOnline = selectedConversation ? onlineUsers[selectedConversation.otherParticipantId] : false;

  return (
    <Fade in={true}>
      <Paper
        elevation={8}
        sx={{
          position: 'fixed',
          bottom: 0,
          right: 20,
          width: 360,
          height: 500,
          display: 'flex',
          flexDirection: 'column',
          borderRadius: '12px 12px 0 0',
          overflow: 'hidden',
          zIndex: 1300,
          boxShadow: '0 -2px 20px rgba(0,0,0,0.15)'
        }}
      >
        {/* Header */}
        <Box
          sx={{
            bgcolor: '#667eea',
            color: 'white',
            p: 2,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between'
          }}
        >
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            {selectedConversation && (
              <IconButton
                size="small"
                onClick={handleBackToList}
                sx={{ color: 'white', mr: 1 }}
              >
                ←
              </IconButton>
            )}
            <Typography variant="h6" fontWeight="600">
              {selectedConversation ? selectedConversation.otherParticipantName || 'User' : 'Messages'}
            </Typography>
            {selectedConversation && (
              <Box
                sx={{
                  width: 8,
                  height: 8,
                  borderRadius: '50%',
                  bgcolor: isOtherUserOnline ? '#10b981' : '#9ca3af',
                  ml: 1
                }}
              />
            )}
          </Box>
          <Box>
            <IconButton size="small" onClick={onOpenFull} sx={{ color: 'white' }}>
              <OpenInFullIcon fontSize="small" />
            </IconButton>
            <IconButton size="small" onClick={onClose} sx={{ color: 'white' }}>
              <CloseIcon fontSize="small" />
            </IconButton>
          </Box>
        </Box>

        {/* Connection Status */}
        {!isConnected && (
          <Box sx={{ bgcolor: '#fef3c7', py: 0.5, px: 2 }}>
            <Typography variant="caption" color="#92400e">
              ⚠️ Connecting to real-time messaging...
            </Typography>
          </Box>
        )}

        {/* Content Area */}
        {selectedConversation ? (
          // Chat View
          <>
            <Box sx={{ flex: 1, overflow: 'auto', p: 2, bgcolor: '#f9fafb' }}>
              {conversationMessages.length === 0 ? (
                <Box sx={{ textAlign: 'center', py: 4 }}>
                  <Typography color="text.secondary">
                    No messages yet. Start the conversation!
                  </Typography>
                </Box>
              ) : (
                conversationMessages.map((msg) => {
                  const isOwn = msg.senderId === currentUserId;
                  return (
                    <Box
                      key={msg.id}
                      sx={{
                        display: 'flex',
                        justifyContent: isOwn ? 'flex-end' : 'flex-start',
                        mb: 1,
                        alignItems: 'flex-end',
                        gap: 0.5
                      }}
                    >
                      {!isOwn && (
                        <Avatar sx={{ width: 24, height: 24, bgcolor: '#667eea', fontSize: '0.7rem' }}>
                          {selectedConversation.otherParticipantName?.charAt(0) || 'U'}
                        </Avatar>
                      )}
                      <Box
                        sx={{
                          maxWidth: '75%',
                          bgcolor: isOwn ? '#0084ff' : '#e4e6eb',
                          color: isOwn ? 'white' : '#050505',
                          px: 2,
                          py: 1.5,
                          borderRadius: isOwn ? '18px 18px 4px 18px' : '18px 18px 18px 4px',
                          boxShadow: '0 1px 2px rgba(0,0,0,0.1)'
                        }}
                      >
                        <Typography variant="body2" sx={{ wordBreak: 'break-word' }}>
                          {msg.content}
                        </Typography>
                        <Typography
                          variant="caption"
                          sx={{
                            display: 'block',
                            mt: 0.5,
                            opacity: 0.7,
                            fontSize: '0.7rem',
                            color: isOwn ? 'rgba(255,255,255,0.85)' : 'rgba(0,0,0,0.5)'
                          }}
                        >
                          {formatDistanceToNow(new Date(msg.sentAt), { addSuffix: true })}
                          {isOwn && msg.readAt && ' • Read'}
                        </Typography>
                      </Box>
                    </Box>
                  );
                })
              )}
              {isOtherUserTyping && (
                <Box sx={{ display: 'flex', alignItems: 'flex-end', gap: 0.5, mt: 1 }}>
                  <Avatar sx={{ width: 24, height: 24, bgcolor: '#667eea', fontSize: '0.7rem' }}>
                    {selectedConversation.otherParticipantName?.charAt(0) || 'U'}
                  </Avatar>
                  <Box
                    sx={{
                      bgcolor: '#e4e6eb',
                      px: 2,
                      py: 1.5,
                      borderRadius: '18px 18px 18px 4px',
                      boxShadow: '0 1px 2px rgba(0,0,0,0.1)'
                    }}
                  >
                    <Typography variant="caption" color="text.secondary" fontStyle="italic">
                      typing...
                    </Typography>
                  </Box>
                </Box>
              )}
              <div ref={messagesEndRef} />
            </Box>

            {/* Message Input */}
            <Box sx={{ p: 1.5, borderTop: 1, borderColor: 'divider', bgcolor: 'white' }}>
              <TextField
                fullWidth
                size="small"
                placeholder="Type a message..."
                value={messageInput}
                onChange={handleInputChange}
                onKeyPress={handleKeyPress}
                disabled={sending}
                InputProps={{
                  endAdornment: (
                    <InputAdornment position="end">
                      <IconButton
                        size="small"
                        onClick={handleSendMessage}
                        disabled={!messageInput.trim() || sending}
                        color="primary"
                      >
                        {sending ? <CircularProgress size={20} /> : <SendIcon />}
                      </IconButton>
                    </InputAdornment>
                  )
                }}
              />
            </Box>
          </>
        ) : (
          // Conversation List View
          <>
            <Box sx={{ p: 1.5, bgcolor: 'white' }}>
              <TextField
                fullWidth
                size="small"
                placeholder="Search conversations..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <SearchIcon fontSize="small" />
                    </InputAdornment>
                  )
                }}
              />
            </Box>

            <List sx={{ flex: 1, overflow: 'auto', py: 0 }}>
              {loading ? (
                <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
                  <CircularProgress />
                </Box>
              ) : filteredConversations.length === 0 ? (
                <Box sx={{ textAlign: 'center', py: 4 }}>
                  <Typography color="text.secondary">
                    {searchQuery ? 'No conversations found' : 'No messages yet'}
                  </Typography>
                </Box>
              ) : (
                filteredConversations.map((conv) => (
                  <React.Fragment key={conv.id}>
                    <ListItem
                      component="div"
                      onClick={() => handleSelectConversation(conv)}
                      sx={{
                        cursor: 'pointer',
                        '&:hover': { bgcolor: 'action.hover' },
                        bgcolor: conv.unreadCount > 0 ? 'action.selected' : 'transparent'
                      }}
                    >
                      <ListItemAvatar>
                        <Badge
                          overlap="circular"
                          anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
                          variant="dot"
                          sx={{
                            '& .MuiBadge-badge': {
                              bgcolor: onlineUsers[conv.otherParticipantId] ? '#10b981' : '#9ca3af',
                              width: 10,
                              height: 10,
                              borderRadius: '50%',
                              border: '2px solid white'
                            }
                          }}
                        >
                          <Avatar>
                            {conv.otherParticipantName?.charAt(0) || 'U'}
                          </Avatar>
                        </Badge>
                      </ListItemAvatar>
                      <ListItemText
                        primary={
                          <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                            <Typography variant="subtitle2" fontWeight={conv.unreadCount > 0 ? 600 : 400}>
                              {conv.otherParticipantName || 'Unknown User'}
                            </Typography>
                            <Typography variant="caption" color="text.secondary">
                              {formatDistanceToNow(new Date(conv.lastMessageAt), { addSuffix: true })}
                            </Typography>
                          </Box>
                        }
                        secondary={
                          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                            <Typography
                              variant="body2"
                              color="text.secondary"
                              noWrap
                              sx={{ maxWidth: '200px', fontWeight: conv.unreadCount > 0 ? 500 : 400 }}
                            >
                              {conv.lastMessagePreview}
                            </Typography>
                            {conv.unreadCount > 0 && (
                              <Badge
                                badgeContent={conv.unreadCount}
                                color="error"
                                sx={{ ml: 1 }}
                              />
                            )}
                          </Box>
                        }
                      />
                    </ListItem>
                    <Divider />
                  </React.Fragment>
                ))
              )}
            </List>
          </>
        )}
      </Paper>
    </Fade>
  );
}
