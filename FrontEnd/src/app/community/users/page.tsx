'use client';

import { useState, useEffect } from 'react';
import {
  Box,
  Container,
  Typography,
  Paper,
  TextField,
  InputAdornment,
  Avatar,
  Chip,
  Grid,
  CircularProgress,
  Alert,
  Card,
  CardContent,
  Stack,
  Button,
  IconButton,
} from '@mui/material';
import {
  Search as SearchIcon,
  PersonAdd as PersonAddIcon,
  Email as EmailIcon,
  FilterList as FilterListIcon,
} from '@mui/icons-material';
import { getAllUsers } from '@/services/admin.service';
import { User } from '@/types/user';

export default function UsersPage() {
  const [users, setUsers] = useState<User[]>([]);
  const [filteredUsers, setFilteredUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [roleFilter, setRoleFilter] = useState<string>('all');

  useEffect(() => {
    fetchUsers();
  }, []);

  useEffect(() => {
    filterUsers();
  }, [searchQuery, roleFilter, users]);

  const fetchUsers = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await getAllUsers();
      setUsers(data);
      setFilteredUsers(data);
    } catch (err: any) {
      console.error('Error fetching users:', err);
      setError(err.message || 'Failed to load users');
    } finally {
      setLoading(false);
    }
  };

  const filterUsers = () => {
    let filtered = users;

    // Search filter
    if (searchQuery.trim()) {
      const query = searchQuery.toLowerCase();
      filtered = filtered.filter(
        (user) =>
          user.username.toLowerCase().includes(query) ||
          user.email.toLowerCase().includes(query)
      );
    }

    // Role filter
    if (roleFilter !== 'all') {
      filtered = filtered.filter((user) =>
        user.roles && Array.isArray(user.roles) && user.roles.some((role) => role.toLowerCase() === roleFilter.toLowerCase())
      );
    }

    setFilteredUsers(filtered);
  };

  const getUniqueRoles = () => {
    const rolesSet = new Set<string>();
    users.forEach((user) => {
      if (user.roles && Array.isArray(user.roles)) {
        user.roles.forEach((role) => rolesSet.add(role));
      }
    });
    return Array.from(rolesSet);
  };

  const getRoleColor = (role: string) => {
    const roleLower = role.toLowerCase();
    if (roleLower.includes('admin')) return '#0366d6';
    if (roleLower.includes('moderator')) return '#6f42c1';
    if (roleLower.includes('researcher')) return '#28a745';
    return '#586069';
  };

  const getInitials = (username: string) => {
    return username
      .split(' ')
      .map((n) => n[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);
  };

  if (loading) {
    return (
      <Box sx={{ minHeight: '100vh', bgcolor: '#fafbfc', py: 6 }}>
        <Container maxWidth="xl">
          <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '60vh' }}>
            <CircularProgress size={48} sx={{ color: '#0366d6' }} />
          </Box>
        </Container>
      </Box>
    );
  }

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: '#fafbfc', py: 6 }}>
      <Container maxWidth="xl">
        {/* Header */}
        <Box sx={{ mb: 6 }}>
          <Typography
            variant="h3"
            sx={{
              color: '#24292e',
              fontWeight: 700,
              mb: 2,
              fontSize: { xs: '2rem', md: '2.5rem' },
            }}
          >
            Community Members
          </Typography>
          <Typography variant="body1" sx={{ color: '#586069', fontSize: '1.125rem' }}>
            Connect with researchers and satellite imagery experts
          </Typography>
        </Box>

        {error && (
          <Alert severity="error" sx={{ mb: 3, borderRadius: 1 }}>
            {error}
          </Alert>
        )}

        {/* Search and Filter */}
        <Paper sx={{ p: 3, mb: 4, border: '1px solid #e1e4e8', borderRadius: 1, boxShadow: 'none' }}>
          <Grid container spacing={2} alignItems="center">
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                placeholder="Search by name or email..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <SearchIcon sx={{ color: '#586069' }} />
                    </InputAdornment>
                  ),
                }}
                sx={{
                  '& .MuiOutlinedInput-root': {
                    bgcolor: 'white',
                    '& fieldset': { borderColor: '#e1e4e8' },
                    '&:hover fieldset': { borderColor: '#0366d6' },
                    '&.Mui-focused fieldset': { borderColor: '#0366d6' },
                  },
                }}
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                <Chip
                  label={`All (${users.length})`}
                  onClick={() => setRoleFilter('all')}
                  sx={{
                    bgcolor: roleFilter === 'all' ? '#0366d6' : '#f6f8fa',
                    color: roleFilter === 'all' ? 'white' : '#586069',
                    fontWeight: 500,
                    border: '1px solid #e1e4e8',
                    '&:hover': {
                      bgcolor: roleFilter === 'all' ? '#0256c9' : '#e1e4e8',
                    },
                  }}
                />
                {getUniqueRoles().map((role) => (
                  <Chip
                    key={role}
                    label={role}
                    onClick={() => setRoleFilter(role === roleFilter ? 'all' : role)}
                    sx={{
                      bgcolor: roleFilter === role ? getRoleColor(role) : '#f6f8fa',
                      color: roleFilter === role ? 'white' : '#586069',
                      fontWeight: 500,
                      border: '1px solid #e1e4e8',
                      '&:hover': {
                        bgcolor: roleFilter === role ? getRoleColor(role) : '#e1e4e8',
                      },
                    }}
                  />
                ))}
              </Stack>
            </Grid>
          </Grid>
        </Paper>

        {/* Users Grid */}
        {filteredUsers.length === 0 ? (
          <Paper sx={{ p: 6, textAlign: 'center', border: '1px solid #e1e4e8', borderRadius: 1, boxShadow: 'none' }}>
            <Typography variant="h6" sx={{ color: '#586069', mb: 1 }}>
              No users found
            </Typography>
            <Typography variant="body2" sx={{ color: '#586069' }}>
              {searchQuery || roleFilter !== 'all'
                ? 'Try adjusting your search or filters'
                : 'No users available'}
            </Typography>
          </Paper>
        ) : (
          <Grid container spacing={3}>
            {filteredUsers.map((user) => (
              <Grid item xs={12} sm={6} md={4} lg={3} key={user.id}>
                <Card
                  sx={{
                    height: '100%',
                    border: '1px solid #e1e4e8',
                    borderRadius: 1,
                    boxShadow: 'none',
                    transition: 'all 0.2s',
                    '&:hover': {
                      borderColor: '#0366d6',
                      boxShadow: '0 3px 8px rgba(3, 102, 214, 0.15)',
                      transform: 'translateY(-2px)',
                    },
                  }}
                >
                  <CardContent sx={{ p: 3, textAlign: 'center' }}>
                    <Avatar
                      sx={{
                        width: 80,
                        height: 80,
                        bgcolor: user.enabled ? '#0366d6' : '#959da5',
                        fontSize: '1.5rem',
                        fontWeight: 600,
                        mx: 'auto',
                        mb: 2,
                      }}
                    >
                      {getInitials(user.username)}
                    </Avatar>

                    <Typography
                      variant="h6"
                      sx={{
                        fontWeight: 600,
                        color: '#24292e',
                        mb: 0.5,
                        fontSize: '1.125rem',
                      }}
                    >
                      {user.username}
                    </Typography>

                    <Typography
                      variant="body2"
                      sx={{
                        color: '#586069',
                        mb: 2,
                        fontSize: '0.875rem',
                        wordBreak: 'break-word',
                      }}
                    >
                      {user.email}
                    </Typography>

                    <Stack direction="row" spacing={0.5} justifyContent="center" flexWrap="wrap" useFlexGap sx={{ mb: 2 }}>
                      {user.roles && Array.isArray(user.roles) && user.roles.map((role) => (
                        <Chip
                          key={role}
                          label={role}
                          size="small"
                          sx={{
                            bgcolor: getRoleColor(role),
                            color: 'white',
                            fontSize: '0.75rem',
                            height: 24,
                            fontWeight: 500,
                          }}
                        />
                      ))}
                    </Stack>

                    {!user.enabled && (
                      <Chip
                        label="Inactive"
                        size="small"
                        sx={{
                          bgcolor: '#ffeef0',
                          color: '#d73a49',
                          fontSize: '0.75rem',
                          fontWeight: 500,
                        }}
                      />
                    )}

                    <Button
                      variant="outlined"
                      size="small"
                      startIcon={<EmailIcon />}
                      href={`mailto:${user.email}`}
                      sx={{
                        mt: 2,
                        textTransform: 'none',
                        borderColor: '#e1e4e8',
                        color: '#586069',
                        fontWeight: 500,
                        '&:hover': {
                          borderColor: '#0366d6',
                          color: '#0366d6',
                          bgcolor: '#f6f8fa',
                        },
                      }}
                    >
                      Contact
                    </Button>
                  </CardContent>
                </Card>
              </Grid>
            ))}
          </Grid>
        )}

        {/* Stats */}
        <Paper sx={{ p: 3, mt: 4, border: '1px solid #e1e4e8', borderRadius: 1, boxShadow: 'none' }}>
          <Typography variant="body2" sx={{ color: '#586069', textAlign: 'center' }}>
            Showing <strong>{filteredUsers.length}</strong> of <strong>{users.length}</strong> members
          </Typography>
        </Paper>
      </Container>
    </Box>
  );
}
