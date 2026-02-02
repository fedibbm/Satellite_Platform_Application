'use client';

import { useEffect, useState } from 'react';
import { 
  Box, 
  Container, 
  Typography, 
  Paper, 
  Card, 
  CardContent, 
  CardActions, 
  Button, 
  Grid,
  Chip,
  CircularProgress,
  Alert,
  Pagination,
  IconButton,
  TextField,
  InputAdornment,
  Avatar,
  Skeleton,
  Stack,
  Divider
} from '@mui/material';
import { Favorite, FavoriteBorder, Visibility, Search, Add, AccessTime, Article } from '@mui/icons-material';
import { useRouter } from 'next/navigation';
import publicationService, { Publication } from '@/services/publicationService';

export default function PublicationsPage() {
  const router = useRouter();
  const [publications, setPublications] = useState<Publication[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedTag, setSelectedTag] = useState<string | null>(null);

  const fetchPublications = async (pageNum: number) => {
    try {
      setLoading(true);
      setError(null);
      const response = await publicationService.getAllPublications({
        page: pageNum,
        size: 9,
        sortBy: 'createdAt',
        sortDir: 'DESC'
      });
      setPublications(response.content);
      setTotalPages(response.totalPages);
    } catch (err: any) {
      console.error('Error fetching publications:', err);
      setError('Failed to load publications. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPublications(page);
  }, [page]);

  const handleLike = async (id: string) => {
    try {
      await publicationService.toggleLike(id);
      fetchPublications(page);
    } catch (err) {
      console.error('Error toggling like:', err);
    }
  };

  const handlePageChange = (event: React.ChangeEvent<unknown>, value: number) => {
    setPage(value - 1);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const filteredPublications = publications.filter(pub => {
    const matchesSearch = searchQuery === '' || 
      pub.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      pub.description?.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesTag = !selectedTag || pub.tags.includes(selectedTag);
    return matchesSearch && matchesTag;
  });

  const allTags = Array.from(new Set(publications.flatMap(pub => pub.tags)));

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  };

  if (loading && publications.length === 0) {
    return (
      <Box sx={{ minHeight: '100vh', bgcolor: '#fafbfc', py: 8 }}>
        <Container maxWidth="lg">
          <Skeleton variant="rectangular" height={60} sx={{ mb: 4, bgcolor: '#e1e4e8', borderRadius: 1 }} />
          <Grid container spacing={3}>
            {[1, 2, 3, 4, 5, 6].map((i) => (
              <Grid item xs={12} md={6} key={i}>
                <Skeleton variant="rectangular" height={280} sx={{ borderRadius: 1, bgcolor: '#e1e4e8' }} />
              </Grid>
            ))}
          </Grid>
        </Container>
      </Box>
    );
  }

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: '#fafbfc', py: 8 }}>
      <Container maxWidth="lg">
        {/* Header Section */}
        <Box sx={{ mb: 6 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 4, flexWrap: 'wrap', gap: 3 }}>
            <Box>
              <Typography variant="h4" sx={{ color: '#24292e', fontWeight: 600, mb: 1, letterSpacing: '-0.02em' }}>
                Research Publications
              </Typography>
              <Typography variant="body1" sx={{ color: '#586069', display: 'flex', alignItems: 'center', gap: 1 }}>
                <Article sx={{ fontSize: 18 }} />
                Share and discover satellite analysis research
              </Typography>
            </Box>
            <Button 
              variant="outlined"
              startIcon={<Add />}
              sx={{ 
                borderColor: '#d1d5da',
                color: '#24292e',
                fontWeight: 500,
                px: 3,
                py: 1,
                textTransform: 'none',
                '&:hover': { 
                  borderColor: '#24292e',
                  bgcolor: '#f6f8fa'
                }
              }}
              onClick={() => router.push('/community/publications/create')}>
              New Publication
            </Button>
          </Box>

          {/* Search and Filter Bar */}
          <Paper sx={{ p: 2, mb: 3, border: '1px solid #e1e4e8', boxShadow: 'none', borderRadius: 1 }}>
            <TextField
              fullWidth
              placeholder="Search publications by title or description..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              size="small"
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <Search sx={{ color: '#586069', fontSize: 20 }} />
                  </InputAdornment>
                ),
              }}
              sx={{
                '& .MuiOutlinedInput-root': {
                  '& fieldset': { borderColor: 'transparent' },
                  '&:hover fieldset': { borderColor: 'transparent' },
                  '&.Mui-focused fieldset': { borderColor: 'transparent' },
                },
              }}
            />
          </Paper>

          {/* Tags Filter */}
          {allTags.length > 0 && (
            <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', mb: 4 }}>
              <Chip
                label="All Topics"
                onClick={() => setSelectedTag(null)}
                size="small"
                sx={{
                  bgcolor: !selectedTag ? '#24292e' : '#f6f8fa',
                  color: !selectedTag ? 'white' : '#586069',
                  fontWeight: 500,
                  border: '1px solid #e1e4e8',
                  '&:hover': { bgcolor: !selectedTag ? '#1b1f23' : '#e1e4e8' },
                }}
              />
              {allTags.map((tag) => (
                <Chip
                  key={tag}
                  label={tag}
                  size="small"
                  onClick={() => setSelectedTag(tag === selectedTag ? null : tag)}
                  sx={{
                    bgcolor: selectedTag === tag ? '#24292e' : '#f6f8fa',
                    color: selectedTag === tag ? 'white' : '#586069',
                    fontWeight: 500,
                    border: '1px solid #e1e4e8',
                    '&:hover': { bgcolor: selectedTag === tag ? '#1b1f23' : '#e1e4e8' },
                  }}
                />
              ))}
            </Box>
          )}
        </Box>

        {error && <Alert severity="error" sx={{ mb: 3, borderRadius: 1, border: '1px solid #d73a49' }}>{error}</Alert>}

        {filteredPublications.length === 0 && !loading ? (
          <Paper sx={{ p: 8, textAlign: 'center', border: '1px solid #e1e4e8', boxShadow: 'none', borderRadius: 1 }}>
            <Typography variant="h6" color="#24292e" gutterBottom fontWeight={600}>
              {searchQuery || selectedTag ? 'No publications found' : 'No publications yet'}
            </Typography>
            <Typography variant="body2" color="#586069" sx={{ mb: 4, maxWidth: 500, mx: 'auto' }}>
              {searchQuery || selectedTag 
                ? 'Try adjusting your search criteria or filters to find relevant publications'
                : 'Start sharing your satellite analysis research and insights with the research community'}
            </Typography>
            {!searchQuery && !selectedTag && (
              <Button 
                variant="outlined"
                onClick={() => router.push('/community/publications/create')}
                sx={{ borderColor: '#d1d5da', color: '#24292e', '&:hover': { borderColor: '#24292e', bgcolor: '#f6f8fa' } }}>
                Create your first publication
              </Button>
            )}
          </Paper>
        ) : (
          <>
            <Stack spacing={2}>
              {filteredPublications.map((pub) => (
                <Paper 
                  key={pub.id}
                  sx={{ 
                    p: 3,
                    border: '1px solid #e1e4e8',
                    borderRadius: 1,
                    boxShadow: 'none',
                    transition: 'all 0.2s ease',
                    cursor: 'pointer',
                    '&:hover': { 
                      borderColor: '#0366d6',
                      bgcolor: '#f6f8fa'
                    }
                  }}
                  onClick={() => router.push(`/community/publications/${pub.id}`)}>
                  <Grid container spacing={3}>
                    {/* Left side - Featured Image */}
                    {pub.featuredImage ? (
                      <Grid item xs={12} sm={3}>
                        <Box 
                          sx={{ 
                            width: '100%',
                            height: 140,
                            backgroundImage: `url(${pub.featuredImage})`, 
                            backgroundSize: 'cover', 
                            backgroundPosition: 'center',
                            border: '1px solid #e1e4e8',
                            borderRadius: 1
                          }} 
                        />
                      </Grid>
                    ) : null}
                    
                    {/* Right side - Content */}
                    <Grid item xs={12} sm={pub.featuredImage ? 9 : 12}>
                      <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
                        {/* Title and Description */}
                        <Box sx={{ flexGrow: 1, mb: 2 }}>
                          <Typography variant="h6" gutterBottom fontWeight={600} sx={{ 
                            color: '#24292e',
                            fontSize: '1.25rem',
                            mb: 1,
                            lineHeight: 1.3
                          }}>
                            {pub.title}
                          </Typography>
                          <Typography variant="body2" sx={{ 
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                            display: '-webkit-box',
                            WebkitLineClamp: 2,
                            WebkitBoxOrient: 'vertical',
                            lineHeight: 1.6,
                            color: '#586069',
                            mb: 2
                          }}>
                            {pub.description || 'No description available'}
                          </Typography>
                          
                          {/* Tags */}
                          <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.75 }}>
                            {pub.tags.slice(0, 4).map((tag, idx) => (
                              <Chip 
                                key={idx} 
                                label={tag} 
                                size="small" 
                                sx={{ 
                                  bgcolor: '#f1f8ff',
                                  color: '#0366d6',
                                  fontWeight: 500,
                                  fontSize: '0.75rem',
                                  height: 22,
                                  border: '1px solid #c8e1ff'
                                }}
                              />
                            ))}
                            {pub.tags.length > 4 && (
                              <Chip label={`+${pub.tags.length - 4}`} size="small" sx={{ bgcolor: '#f6f8fa', color: '#586069', height: 22 }} />
                            )}
                          </Box>
                        </Box>
                        
                        {/* Footer - Author, Date, and Stats */}
                        <Box sx={{ 
                          display: 'flex', 
                          alignItems: 'center', 
                          justifyContent: 'space-between',
                          pt: 2,
                          borderTop: '1px solid #e1e4e8',
                          flexWrap: 'wrap',
                          gap: 2
                        }}>
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                              <Avatar sx={{ width: 28, height: 28, bgcolor: '#24292e', fontSize: '0.875rem' }}>
                                {pub.author.name.charAt(0)}
                              </Avatar>
                              <Box>
                                <Typography variant="caption" fontWeight={600} color="#24292e" display="block">
                                  {pub.author.name}
                                </Typography>
                                <Typography variant="caption" color="#586069">
                                  {formatDate(pub.createdAt)}
                                </Typography>
                              </Box>
                            </Box>
                          </Box>
                          
                          <Stack direction="row" spacing={3} alignItems="center">
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75 }}>
                              <IconButton 
                                size="small" 
                                onClick={(e) => {
                                  e.stopPropagation();
                                  handleLike(pub.id);
                                }}
                                sx={{ 
                                  p: 0.5,
                                  '&:hover': { bgcolor: 'rgba(3, 102, 214, 0.1)' } 
                                }}>
                                {pub.isLikedByCurrentUser ? 
                                  <Favorite sx={{ fontSize: 18, color: '#0366d6' }} /> : 
                                  <FavoriteBorder sx={{ fontSize: 18, color: '#586069' }} />}
                              </IconButton>
                              <Typography variant="caption" fontWeight={500} color="#586069">{pub.likeCount}</Typography>
                            </Box>
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75 }}>
                              <Visibility sx={{ fontSize: 18, color: '#586069' }} />
                              <Typography variant="caption" fontWeight={500} color="#586069">{pub.viewCount}</Typography>
                            </Box>
                            {pub.readingTime && (
                              <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75 }}>
                                <AccessTime sx={{ fontSize: 16, color: '#586069' }} />
                                <Typography variant="caption" fontWeight={500} color="#586069">  
                                  {pub.readingTime} min
                                </Typography>
                              </Box>
                            )}
                          </Stack>
                        </Box>
                      </Box>
                    </Grid>
                  </Grid>
                </Paper>
              ))}
            </Stack>

            {totalPages > 1 && (
              <Box sx={{ display: 'flex', justifyContent: 'center', mt: 6 }}>
                <Pagination 
                  count={totalPages} 
                  page={page + 1} 
                  onChange={handlePageChange}
                  sx={{ 
                    '& .MuiPaginationItem-root': { 
                      fontWeight: 500,
                      fontSize: '0.875rem',
                      color: '#586069',
                      border: '1px solid #e1e4e8',
                      '&:hover': {
                        bgcolor: '#f6f8fa'
                      },
                      '&.Mui-selected': {
                        bgcolor: '#24292e',
                        color: 'white',
                        border: '1px solid #24292e',
                        '&:hover': {
                          bgcolor: '#1b1f23'
                        }
                      }
                    }
                  }} 
                />
              </Box>
            )}
          </>
        )}
      </Container>
    </Box>
  );
}
