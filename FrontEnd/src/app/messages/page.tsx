'use client';

import React, { useState, useEffect, useRef } from 'react';
import {
  Box,
  Paper,
  Typography,
  List,
  ListItem,
  ListItemAvatar,
  ListItemText,
  Avatar,
  TextField,
  InputAdornment,
  IconButton,
  Badge,
  Divider,
  CircularProgress
} from '@mui/material';
import {
  Send as SendIcon,
  Search as SearchIcon
} from '@mui/icons-material';
import { useMessaging } from '@/hooks/useMessaging';
import { Conversation } from '@/types/messaging';
import { formatDistanceToNow } from 'date-fns';

export default function MessagesPage() {
  const {
    conversations,
    messages,
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

  // Auto-scroll to bottom when messages change
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, selectedConversation]);

  // Load messages when conversation selected
  useEffect(() => {
    if (selectedConversation) {
      loadMessages(selectedConversation.id);
    }
  }, [selectedConversation, loadMessages]);

  const handleSelectConversation = (conversation: Conversation) => {
    setSelectedConversation(conversation);
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

    if (selectedConversation) {
      sendTyping(selectedConversation.otherParticipantId, true);

      // Stop typing after 3 seconds of inactivity
      if (typingTimeoutRef.current) {
        clearTimeout(typingTimeoutRef.current);
      }

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

  // Filter conversations based on search query
  const filteredConversations = conversations.filter(conv =>
    conv.otherParticipantName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
    conv.lastMessagePreview.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const currentUserId = getCurrentUserId();
  const conversationMessages = selectedConversation ? messages[selectedConversation.id] || [] : [];
  const isOtherUserTyping = selectedConversation ? typingUsers[selectedConversation.otherParticipantId] : false;
  const isOtherUserOnline = selectedConversation ? onlineUsers[selectedConversation.otherParticipantId] : false;

  return (
    <Box sx={{ height: 'calc(100vh - 80px)', display: 'flex', bgcolor: '#f5f5f5' }}>
      {/* Left Sidebar - Conversations List */}
      <Paper
        sx={{
          width: 360,
          borderRadius: 0,
          borderRight: 1,
          borderColor: 'divider',
          display: 'flex',
          flexDirection: 'column'
        }}
      >
        {/* Header */}
        <Box sx={{ p: 2, borderBottom: 1, borderColor: 'divider' }}>
          <Typography variant="h5" fontWeight="600">
            Messages
          </Typography>
          {!isConnected && (
            <Typography variant="caption" color="warning.main">
              ⚠️ Connecting...
            </Typography>
          )}
        </Box>

        {/* Search */}
        <Box sx={{ p: 2 }}>
          <TextField
            fullWidth
            size="small"
            placeholder="Search conversations..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon />
                </InputAdornment>
              )
            }}
          />
        </Box>

        {/* Conversations List */}
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
                  selected={selectedConversation?.id === conv.id}
                  onClick={() => handleSelectConversation(conv)}
                  sx={{
                    cursor: 'pointer',
                    '&:hover': { bgcolor: 'action.hover' },
                    bgcolor: conv.unreadCount > 0 && selectedConversation?.id !== conv.id
                      ? 'action.selected'
                      : 'transparent'
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
                      <Avatar sx={{ width: 48, height: 48 }}>
                        {conv.otherParticipantName?.charAt(0) || 'U'}
                      </Avatar>
                    </Badge>
                  </ListItemAvatar>
                  <ListItemText
                    primary={
                      <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                        <Typography variant="subtitle1" fontWeight={conv.unreadCount > 0 ? 600 : 400}>
                          {conv.otherParticipantName || 'Unknown User'}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          {formatDistanceToNow(new Date(conv.lastMessageAt), { addSuffix: true })}
                        </Typography>
                      </Box>
                    }
                    secondary={
                      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mt: 0.5 }}>
                        <Typography
                          variant="body2"
                          color="text.secondary"
                          noWrap
                          sx={{ maxWidth: '220px', fontWeight: conv.unreadCount > 0 ? 500 : 400 }}
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
      </Paper>

      {/* Right Side - Chat Area */}
      <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
        {selectedConversation ? (
          <>
            {/* Chat Header */}
            <Paper
              sx={{
                p: 2,
                borderRadius: 0,
                borderBottom: 1,
                borderColor: 'divider',
                display: 'flex',
                alignItems: 'center',
                gap: 2
              }}
            >
              <Avatar sx={{ width: 48, height: 48 }}>
                {selectedConversation.otherParticipantName?.charAt(0) || 'U'}
              </Avatar>
              <Box sx={{ flex: 1 }}>
                <Typography variant="h6">
                  {selectedConversation.otherParticipantName || 'Unknown User'}
                </Typography>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <Box
                    sx={{
                      width: 8,
                      height: 8,
                      borderRadius: '50%',
                      bgcolor: isOtherUserOnline ? '#10b981' : '#9ca3af'
                    }}
                  />
                  <Typography variant="caption" color="text.secondary">
                    {isOtherUserOnline ? 'Online' : 'Offline'}
                  </Typography>
                </Box>
              </Box>
            </Paper>

            {/* Messages Area */}
            <Box sx={{ flex: 1, overflow: 'auto', p: 3, bgcolor: '#f9fafb' }}>
              {conversationMessages.length === 0 ? (
                <Box sx={{ textAlign: 'center', py: 8 }}>
                  <Typography color="text.secondary" variant="h6">
                    No messages yet
                  </Typography>
                  <Typography color="text.secondary" variant="body2">
                    Start the conversation!
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
                        mb: 2,
                        alignItems: 'flex-end',
                        gap: 0.5
                      }}
                    >
                      {!isOwn && (
                        <Avatar sx={{ width: 32, height: 32, bgcolor: '#667eea' }}>
                          {selectedConversation.otherParticipantName?.charAt(0) || 'U'}
                        </Avatar>
                      )}
                      <Box
                        sx={{
                          maxWidth: '60%',
                          bgcolor: isOwn ? '#0084ff' : '#e4e6eb',
                          color: isOwn ? 'white' : '#050505',
                          px: 2.5,
                          py: 1.5,
                          borderRadius: isOwn ? '18px 18px 4px 18px' : '18px 18px 18px 4px',
                          boxShadow: '0 1px 2px rgba(0,0,0,0.1)'
                        }}
                      >
                        <Typography variant="body1" sx={{ wordBreak: 'break-word' }}>
                          {msg.content}
                        </Typography>
                        <Typography
                          variant="caption"
                          sx={{
                            display: 'block',
                            mt: 0.5,
                            opacity: 0.8,
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
                <Box sx={{ display: 'flex', alignItems: 'flex-end', gap: 0.5, mb: 2 }}>
                  <Avatar sx={{ width: 32, height: 32, bgcolor: '#667eea' }}>
                    {selectedConversation.otherParticipantName?.charAt(0) || 'U'}
                  </Avatar>
                  <Box
                    sx={{
                      bgcolor: '#e4e6eb',
                      px: 2.5,
                      py: 1.5,
                      borderRadius: '18px 18px 18px 4px',
                      boxShadow: '0 1px 2px rgba(0,0,0,0.1)'
                    }}
                  >
                    <Typography variant="body2" color="text.secondary" fontStyle="italic">
                      typing...
                    </Typography>
                  </Box>
                </Box>
              )}
              <div ref={messagesEndRef} />
            </Box>

            {/* Message Input */}
            <Paper
              sx={{
                p: 2,
                borderRadius: 0,
                borderTop: 1,
                borderColor: 'divider'
              }}
            >
              <TextField
                fullWidth
                multiline
                maxRows={4}
                placeholder="Type a message..."
                value={messageInput}
                onChange={handleInputChange}
                onKeyPress={handleKeyPress}
                disabled={sending}
                InputProps={{
                  endAdornment: (
                    <InputAdornment position="end">
                      <IconButton
                        onClick={handleSendMessage}
                        disabled={!messageInput.trim() || sending}
                        color="primary"
                      >
                        {sending ? <CircularProgress size={24} /> : <SendIcon />}
                      </IconButton>
                    </InputAdornment>
                  )
                }}
              />
            </Paper>
          </>
        ) : (
          <Box
            sx={{
              flex: 1,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexDirection: 'column',
              gap: 2
            }}
          >
            <Typography variant="h5" color="text.secondary">
              Select a conversation to start messaging
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Choose from existing conversations or search for a user
            </Typography>
          </Box>
        )}
      </Box>
    </Box>
  );
}
