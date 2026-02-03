'use client'

import { useState, useEffect } from 'react'
import { useAuth } from '@/hooks/useAuth'
import { useRouter } from 'next/navigation'
import {
  Box,
  Container,
  Paper,
  Typography,
  Avatar,
  TextField,
  Button,
  Chip,
  Divider,
  Alert,
  CircularProgress,
  Grid,
  Stack,
  Card,
  CardContent,
  CardActions,
  Pagination,
} from '@mui/material'
import EditIcon from '@mui/icons-material/Edit'
import SaveIcon from '@mui/icons-material/Save'
import CancelIcon from '@mui/icons-material/Cancel'
import PersonIcon from '@mui/icons-material/Person'
import SecurityIcon from '@mui/icons-material/Security'
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings'
import AccountCircleIcon from '@mui/icons-material/AccountCircle'
import ArticleIcon from '@mui/icons-material/Article'
import VisibilityIcon from '@mui/icons-material/Visibility'
import ThumbUpIcon from '@mui/icons-material/ThumbUp'
import CommentIcon from '@mui/icons-material/Comment'
import publicationService, { Publication } from '@/services/publicationService'

export default function ProfilePage() {
  const { user, isLoggedIn } = useAuth()
  const router = useRouter()
  const [isEditing, setIsEditing] = useState(false)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  
  // Publications state
  const [publications, setPublications] = useState<Publication[]>([])
  const [loadingPublications, setLoadingPublications] = useState(false)
  const [publicationsPage, setPublicationsPage] = useState(0)
  const [totalPublications, setTotalPublications] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const publicationsPerPage = 5
  
  // Form state
  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')

  useEffect(() => {
    if (!isLoggedIn()) {
      router.push('/auth/login')
      return
    }

    if (user) {
      setUsername(user.username || '')
      setEmail(user.email || '')
      setLoading(false)
      // Load publications on initial mount
      loadPublications()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user, isLoggedIn, router])

  useEffect(() => {
    if (user && !loading) {
      loadPublications()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [publicationsPage])

  const loadPublications = async () => {
    if (!user?.email) {
      console.log('No user email available')
      return
    }
    
    setLoadingPublications(true)
    try {
      console.log('Loading publications for page:', publicationsPage)
      const response = await publicationService.getMyPublications({
        page: publicationsPage,
        size: publicationsPerPage,
      })
      console.log('Publications response:', response)
      setPublications(response.content)
      setTotalPublications(response.totalElements)
      setTotalPages(response.totalPages)
    } catch (err: any) {
      console.error('Failed to load publications:', err)
      console.error('Error details:', err.response?.data || err.message)
      setError('Failed to load publications. Please try refreshing the page.')
    } finally {
      setLoadingPublications(false)
    }
  }

  const handlePublicationsPageChange = (event: React.ChangeEvent<unknown>, value: number) => {
    setPublicationsPage(value - 1)
  }

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'PUBLISHED':
        return { bgcolor: '#d4edda', color: '#155724' }
      case 'DRAFT':
        return { bgcolor: '#fff3cd', color: '#856404' }
      case 'ARCHIVED':
        return { bgcolor: '#f8d7da', color: '#721c24' }
      default:
        return { bgcolor: '#e2e3e5', color: '#383d41' }
    }
  }

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    })
  }

  const handleEditToggle = () => {
    if (isEditing) {
      // Cancel editing - reset values
      setUsername(user?.username || '')
      setEmail(user?.email || '')
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
      setError('')
    }
    setIsEditing(!isEditing)
  }

  const handleSave = async () => {
    setError('')
    setSuccess('')

    // Validate password change if attempting
    if (newPassword || confirmPassword) {
      if (!currentPassword) {
        setError('Current password is required to change password')
        return
      }
      if (newPassword !== confirmPassword) {
        setError('New passwords do not match')
        return
      }
      if (newPassword.length < 8) {
        setError('Password must be at least 8 characters long')
        return
      }
    }

    setSaving(true)

    try {
      // Here you would call your API to update the user profile
      // For now, just simulate a successful save
      await new Promise(resolve => setTimeout(resolve, 1000))
      
      setSuccess('Profile updated successfully!')
      setIsEditing(false)
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
    } catch (err: any) {
      setError(err.message || 'Failed to update profile')
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return (
      <Box
        sx={{
          minHeight: '100vh',
          bgcolor: '#f8f9fa',
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
        }}
      >
        <CircularProgress />
      </Box>
    )
  }

  return (
    <Box
      sx={{
        minHeight: '100vh',
        bgcolor: '#f8f9fa',
        py: 4,
      }}
    >
      <Container maxWidth="lg">
        {/* Page Header */}
        <Box sx={{ mb: 4 }}>
          <Typography
            variant="h4"
            sx={{
              fontWeight: 600,
              color: '#1a1a1a',
              mb: 0.5,
            }}
          >
            User Profile
          </Typography>
          <Typography
            variant="body1"
            sx={{
              color: '#6c757d',
            }}
          >
            Manage your personal information and account settings
          </Typography>
        </Box>

        {/* Alerts */}
        {error && (
          <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError('')}>
            {error}
          </Alert>
        )}
        {success && (
          <Alert severity="success" sx={{ mb: 3 }} onClose={() => setSuccess('')}>
            {success}
          </Alert>
        )}

        <Grid container spacing={3}>
          {/* Left Column - Profile Summary */}
          <Grid item xs={12} md={4}>
            <Stack spacing={3}>
              {/* Avatar & Basic Info Card */}
              <Paper
                elevation={0}
                sx={{
                  border: '1px solid #dee2e6',
                  borderRadius: 2,
                  p: 3,
                }}
              >
                <Box sx={{ textAlign: 'center', mb: 3 }}>
                  <Avatar
                    sx={{
                      width: 120,
                      height: 120,
                      bgcolor: '#1976d2',
                      fontSize: '3rem',
                      fontWeight: 600,
                      mx: 'auto',
                      mb: 2,
                      border: '4px solid #e3f2fd',
                    }}
                  >
                    {username?.charAt(0)?.toUpperCase() || 'U'}
                  </Avatar>

                  <Typography variant="h5" sx={{ fontWeight: 600, color: '#1a1a1a', mb: 0.5 }}>
                    {username}
                  </Typography>
                  <Typography variant="body2" sx={{ color: '#6c757d', mb: 2 }}>
                    {email}
                  </Typography>

                  {!isEditing && (
                    <Button
                      variant="outlined"
                      startIcon={<EditIcon />}
                      onClick={handleEditToggle}
                      fullWidth
                      sx={{
                        borderColor: '#1976d2',
                        color: '#1976d2',
                        textTransform: 'none',
                        fontWeight: 500,
                        py: 1,
                        '&:hover': {
                          borderColor: '#1565c0',
                          bgcolor: '#e3f2fd',
                        },
                      }}
                    >
                      Edit Profile
                    </Button>
                  )}
                </Box>

                <Divider sx={{ my: 2 }} />

                {/* Roles Section */}
                <Box>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
                    <AdminPanelSettingsIcon sx={{ color: '#1976d2', fontSize: 20 }} />
                    <Typography variant="subtitle2" sx={{ fontWeight: 600, color: '#1a1a1a' }}>
                      Access Roles
                    </Typography>
                  </Box>
                  <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                    {user?.roles?.map((role: string) => (
                      <Chip
                        key={role}
                        label={role.replace('ROLE_', '')}
                        size="small"
                        sx={{
                          bgcolor: '#e3f2fd',
                          color: '#1976d2',
                          fontWeight: 500,
                          borderRadius: 1,
                        }}
                      />
                    ))}
                  </Box>
                </Box>
              </Paper>

              {/* Account Details Card */}
              <Paper
                elevation={0}
                sx={{
                  border: '1px solid #dee2e6',
                  borderRadius: 2,
                  p: 3,
                }}
              >
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
                  <AccountCircleIcon sx={{ color: '#1976d2', fontSize: 20 }} />
                  <Typography variant="subtitle2" sx={{ fontWeight: 600, color: '#1a1a1a' }}>
                    Account Information
                  </Typography>
                </Box>
                <Stack spacing={1.5}>
                  <Box>
                    <Typography variant="caption" sx={{ color: '#6c757d', display: 'block', mb: 0.5 }}>
                      User ID
                    </Typography>
                    <Typography variant="body2" sx={{ color: '#1a1a1a', fontWeight: 500 }}>
                      {user?.id}
                    </Typography>
                  </Box>
                  <Box>
                    <Typography variant="caption" sx={{ color: '#6c757d', display: 'block', mb: 0.5 }}>
                      Total Roles
                    </Typography>
                    <Typography variant="body2" sx={{ color: '#1a1a1a', fontWeight: 500 }}>
                      {user?.roles?.length || 0}
                    </Typography>
                  </Box>
                  <Box>
                    <Typography variant="caption" sx={{ color: '#6c757d', display: 'block', mb: 0.5 }}>
                      Account Status
                    </Typography>
                    <Chip
                      label="Active"
                      size="small"
                      sx={{
                        bgcolor: '#d4edda',
                        color: '#155724',
                        fontWeight: 500,
                        height: 24,
                      }}
                    />
                  </Box>
                </Stack>
              </Paper>
            </Stack>
          </Grid>

          {/* Right Column - Edit Forms */}
          <Grid item xs={12} md={8}>
            <Stack spacing={3}>
              {/* Personal Information Card */}
              <Paper
                elevation={0}
                sx={{
                  border: '1px solid #dee2e6',
                  borderRadius: 2,
                  overflow: 'hidden',
                }}
              >
                {/* Card Header */}
                <Box
                  sx={{
                    bgcolor: '#f8f9fa',
                    borderBottom: '1px solid #dee2e6',
                    p: 2.5,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                  }}
                >
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                    <PersonIcon sx={{ color: '#1976d2', fontSize: 24 }} />
                    <Typography variant="h6" sx={{ fontWeight: 600, color: '#1a1a1a' }}>
                      Personal Information
                    </Typography>
                  </Box>
                  {isEditing && (
                    <Box sx={{ display: 'flex', gap: 1 }}>
                      <Button
                        variant="outlined"
                        size="small"
                        startIcon={<CancelIcon />}
                        onClick={handleEditToggle}
                        disabled={saving}
                        sx={{
                          textTransform: 'none',
                          fontWeight: 500,
                        }}
                      >
                        Cancel
                      </Button>
                      <Button
                        variant="contained"
                        size="small"
                        startIcon={saving ? <CircularProgress size={16} color="inherit" /> : <SaveIcon />}
                        onClick={handleSave}
                        disabled={saving}
                        sx={{
                          bgcolor: '#28a745',
                          textTransform: 'none',
                          fontWeight: 500,
                          '&:hover': {
                            bgcolor: '#218838',
                          },
                        }}
                      >
                        {saving ? 'Saving...' : 'Save'}
                      </Button>
                    </Box>
                  )}
                </Box>

                {/* Card Body */}
                <Box sx={{ p: 3 }}>
                  <Stack spacing={3}>
                    <TextField
                      label="Username"
                      value={username}
                      onChange={(e) => setUsername(e.target.value)}
                      disabled={!isEditing || saving}
                      fullWidth
                      InputProps={{
                        readOnly: !isEditing,
                      }}
                      sx={{
                        '& .MuiOutlinedInput-root': {
                          bgcolor: isEditing ? 'white' : '#f8f9fa',
                        },
                      }}
                    />
                    <TextField
                      label="Email Address"
                      type="email"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      disabled={!isEditing || saving}
                      fullWidth
                      InputProps={{
                        readOnly: !isEditing,
                      }}
                      sx={{
                        '& .MuiOutlinedInput-root': {
                          bgcolor: isEditing ? 'white' : '#f8f9fa',
                        },
                      }}
                    />
                  </Stack>
                </Box>
              </Paper>

              {/* Security Settings Card - Only visible when editing */}
              {isEditing && (
                <Paper
                  elevation={0}
                  sx={{
                    border: '1px solid #dee2e6',
                    borderRadius: 2,
                    overflow: 'hidden',
                  }}
                >
                  {/* Card Header */}
                  <Box
                    sx={{
                      bgcolor: '#f8f9fa',
                      borderBottom: '1px solid #dee2e6',
                      p: 2.5,
                    }}
                  >
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 0.5 }}>
                      <SecurityIcon sx={{ color: '#1976d2', fontSize: 24 }} />
                      <Typography variant="h6" sx={{ fontWeight: 600, color: '#1a1a1a' }}>
                        Change Password
                      </Typography>
                    </Box>
                    <Typography variant="caption" sx={{ color: '#6c757d', pl: 4.5 }}>
                      Leave all fields blank to keep your current password
                    </Typography>
                  </Box>

                  {/* Card Body */}
                  <Box sx={{ p: 3 }}>
                    <Stack spacing={3}>
                      <TextField
                        label="Current Password"
                        type="password"
                        value={currentPassword}
                        onChange={(e) => setCurrentPassword(e.target.value)}
                        disabled={saving}
                        fullWidth
                        placeholder="Enter current password"
                      />
                      <TextField
                        label="New Password"
                        type="password"
                        value={newPassword}
                        onChange={(e) => setNewPassword(e.target.value)}
                        disabled={saving}
                        fullWidth
                        placeholder="Enter new password"
                        helperText="Minimum 8 characters required"
                      />
                      <TextField
                        label="Confirm New Password"
                        type="password"
                        value={confirmPassword}
                        onChange={(e) => setConfirmPassword(e.target.value)}
                        disabled={saving}
                        fullWidth
                        placeholder="Confirm new password"
                      />
                    </Stack>
                  </Box>
                </Paper>
              )}
            </Stack>
          </Grid>
        </Grid>

        {/* Publications Section */}
        <Box sx={{ mt: 4 }}>
          <Paper
            elevation={0}
            sx={{
              border: '1px solid #dee2e6',
              borderRadius: 2,
              overflow: 'hidden',
            }}
          >
            {/* Section Header */}
            <Box
              sx={{
                bgcolor: '#f8f9fa',
                borderBottom: '1px solid #dee2e6',
                p: 2.5,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
              }}
            >
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                <ArticleIcon sx={{ color: '#1976d2', fontSize: 24 }} />
                <Box>
                  <Typography variant="h6" sx={{ fontWeight: 600, color: '#1a1a1a' }}>
                    My Publications
                  </Typography>
                  <Typography variant="caption" sx={{ color: '#6c757d' }}>
                    {totalPublications} {totalPublications === 1 ? 'publication' : 'publications'} total
                  </Typography>
                </Box>
              </Box>
              <Box sx={{ display: 'flex', gap: 1 }}>
                <Button
                  variant="outlined"
                  size="small"
                  onClick={() => loadPublications()}
                  disabled={loadingPublications}
                  sx={{
                    textTransform: 'none',
                    fontWeight: 500,
                  }}
                >
                  Refresh
                </Button>
                <Button
                  variant="contained"
                  size="small"
                  onClick={() => router.push('/community/publications/create')}
                  sx={{
                    bgcolor: '#1976d2',
                    textTransform: 'none',
                    fontWeight: 500,
                    '&:hover': {
                      bgcolor: '#1565c0',
                    },
                  }}
                >
                  New Publication
                </Button>
              </Box>
            </Box>

            {/* Publications List */}
            <Box sx={{ p: 3 }}>
              {/* Debug Info */}
              {process.env.NODE_ENV === 'development' && (
                <Alert severity="info" sx={{ mb: 2 }}>
                  <Typography variant="caption" component="div">
                    Debug: User email: {user?.email || 'Not set'}
                  </Typography>
                  <Typography variant="caption" component="div">
                    Total publications: {totalPublications}
                  </Typography>
                  <Typography variant="caption" component="div">
                    Current page: {publicationsPage + 1} of {totalPages}
                  </Typography>
                  <Typography variant="caption" component="div">
                    Publications loaded: {publications.length}
                  </Typography>
                </Alert>
              )}

              {loadingPublications ? (
                <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
                  <CircularProgress />
                </Box>
              ) : publications.length === 0 ? (
                <Box sx={{ textAlign: 'center', py: 6 }}>
                  <ArticleIcon sx={{ fontSize: 64, color: '#dee2e6', mb: 2 }} />
                  <Typography variant="h6" sx={{ color: '#6c757d', mb: 1 }}>
                    No publications yet
                  </Typography>
                  <Typography variant="body2" sx={{ color: '#6c757d', mb: 3 }}>
                    Start sharing your research with the community
                  </Typography>
                  <Button
                    variant="contained"
                    onClick={() => router.push('/community/publications/create')}
                    sx={{
                      bgcolor: '#1976d2',
                      textTransform: 'none',
                      fontWeight: 500,
                      '&:hover': {
                        bgcolor: '#1565c0',
                      },
                    }}
                  >
                    Create Your First Publication
                  </Button>
                </Box>
              ) : (
                <Stack spacing={2}>
                  {publications.map((pub) => (
                    <Card
                      key={pub.id}
                      elevation={0}
                      sx={{
                        border: '1px solid #dee2e6',
                        borderRadius: 2,
                        transition: 'all 0.2s',
                        '&:hover': {
                          borderColor: '#1976d2',
                          boxShadow: '0 2px 8px rgba(25, 118, 210, 0.15)',
                        },
                      }}
                    >
                      <CardContent sx={{ pb: 1 }}>
                        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start', mb: 1 }}>
                          <Typography
                            variant="h6"
                            sx={{
                              fontWeight: 600,
                              color: '#1a1a1a',
                              cursor: 'pointer',
                              '&:hover': { color: '#1976d2' },
                            }}
                            onClick={() => router.push(`/community/publications/${pub.id}`)}
                          >
                            {pub.title}
                          </Typography>
                          <Chip
                            label={pub.status}
                            size="small"
                            sx={{
                              ...getStatusColor(pub.status),
                              fontWeight: 500,
                              fontSize: '0.75rem',
                            }}
                          />
                        </Box>

                        {pub.description && (
                          <Typography
                            variant="body2"
                            sx={{
                              color: '#6c757d',
                              mb: 2,
                              display: '-webkit-box',
                              WebkitLineClamp: 2,
                              WebkitBoxOrient: 'vertical',
                              overflow: 'hidden',
                            }}
                          >
                            {pub.description}
                          </Typography>
                        )}

                        <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', mb: 2 }}>
                          {pub.tags?.slice(0, 3).map((tag) => (
                            <Chip
                              key={tag}
                              label={tag}
                              size="small"
                              sx={{
                                bgcolor: '#f8f9fa',
                                color: '#495057',
                                fontSize: '0.7rem',
                                height: 22,
                              }}
                            />
                          ))}
                          {pub.tags && pub.tags.length > 3 && (
                            <Chip
                              label={`+${pub.tags.length - 3} more`}
                              size="small"
                              sx={{
                                bgcolor: '#f8f9fa',
                                color: '#495057',
                                fontSize: '0.7rem',
                                height: 22,
                              }}
                            />
                          )}
                        </Box>

                        <Box sx={{ display: 'flex', gap: 3, alignItems: 'center' }}>
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                            <VisibilityIcon sx={{ fontSize: 16, color: '#6c757d' }} />
                            <Typography variant="caption" sx={{ color: '#6c757d' }}>
                              {pub.viewCount}
                            </Typography>
                          </Box>
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                            <ThumbUpIcon sx={{ fontSize: 16, color: '#6c757d' }} />
                            <Typography variant="caption" sx={{ color: '#6c757d' }}>
                              {pub.likeCount}
                            </Typography>
                          </Box>
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                            <CommentIcon sx={{ fontSize: 16, color: '#6c757d' }} />
                            <Typography variant="caption" sx={{ color: '#6c757d' }}>
                              {pub.commentCount}
                            </Typography>
                          </Box>
                          <Typography variant="caption" sx={{ color: '#6c757d', ml: 'auto' }}>
                            {pub.publishedAt ? `Published ${formatDate(pub.publishedAt)}` : `Created ${formatDate(pub.createdAt)}`}
                          </Typography>
                        </Box>
                      </CardContent>

                      <CardActions sx={{ px: 2, pb: 2, pt: 0 }}>
                        <Button
                          size="small"
                          onClick={() => router.push(`/community/publications/${pub.id}`)}
                          sx={{ textTransform: 'none', fontWeight: 500 }}
                        >
                          View
                        </Button>
                        <Button
                          size="small"
                          onClick={() => router.push(`/community/publications/${pub.id}/edit`)}
                          sx={{ textTransform: 'none', fontWeight: 500 }}
                        >
                          Edit
                        </Button>
                      </CardActions>
                    </Card>
                  ))}

                  {/* Pagination */}
                  {totalPages > 1 && (
                    <Box sx={{ display: 'flex', justifyContent: 'center', mt: 3 }}>
                      <Pagination
                        count={totalPages}
                        page={publicationsPage + 1}
                        onChange={handlePublicationsPageChange}
                        color="primary"
                      />
                    </Box>
                  )}
                </Stack>
              )}
            </Box>
          </Paper>
        </Box>
      </Container>
    </Box>
  )
}
