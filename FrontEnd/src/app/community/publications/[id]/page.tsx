'use client';

import { useEffect, useState } from 'react';
import dynamic from 'next/dynamic';
import { useParams, useRouter } from 'next/navigation';
import { 
  Box, 
  Container, 
  Typography, 
  Paper, 
  Chip, 
  CircularProgress, 
  Alert, 
  IconButton, 
  Divider, 
  Avatar, 
  Button,
  Stack,
  Skeleton,
  Grid
} from '@mui/material';
import { 
  Favorite, 
  FavoriteBorder, 
  Visibility, 
  ArrowBack, 
  Share, 
  AccessTime
} from '@mui/icons-material';
import 'katex/dist/katex.min.css';
import 'highlight.js/styles/github.css';
import publicationService, { Publication } from '@/services/publicationService';

// Create a wrapper component for ReactMarkdown with all plugins
const MarkdownViewer = dynamic(
  () => import('@/components/MarkdownViewer'),
  {
    ssr: false,
    loading: () => (
      <Box sx={{ p: 3 }}>
        <Skeleton variant="text" height={40} />
        <Skeleton variant="text" height={30} />
        <Skeleton variant="rectangular" height={200} sx={{ mt: 2 }} />
      </Box>
    ),
  }
);

export default function PublicationDetailPage() {
  const params = useParams();
  const router = useRouter();
  const id = params.id as string;
  
  const [publication, setPublication] = useState<Publication | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchPublication = async () => {
      try {
        setLoading(true);
        setError(null);
        const data = await publicationService.getPublicationById(id);
        setPublication(data);
      } catch (err: any) {
        console.error('Error fetching publication:', err);
        setError('Failed to load publication. Please try again.');
      } finally {
        setLoading(false);
      }
    };

    if (id) {
      fetchPublication();
    }
  }, [id]);

  const handleLike = async () => {
    if (!publication) return;
    
    try {
      const updated = await publicationService.toggleLike(publication.id);
      setPublication(updated);
    } catch (err) {
      console.error('Error toggling like:', err);
    }
  };

  const handleShare = () => {
    if (navigator.share) {
      navigator.share({
        title: publication?.title,
        text: publication?.description,
        url: window.location.href,
      });
    } else {
      navigator.clipboard.writeText(window.location.href);
      alert('Link copied to clipboard!');
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  };

  if (loading) {
    return (
      <Box sx={{ minHeight: '100vh', bgcolor: '#fafbfc', py: 6 }}>
        <Container maxWidth="md">
          <Skeleton variant="rectangular" width={120} height={36} sx={{ mb: 3, borderRadius: 1 }} />
          <Paper sx={{ p: 6, border: '1px solid #e1e4e8', boxShadow: 'none', borderRadius: 1 }}>
            <Skeleton variant="text" height={50} sx={{ mb: 2 }} />
            <Skeleton variant="text" width="60%" height={30} sx={{ mb: 3 }} />
            <Skeleton variant="rectangular" height={400} sx={{ mb: 3, borderRadius: 1 }} />
            <Skeleton variant="text" height={200} />
          </Paper>
        </Container>
      </Box>
    );
  }

  if (error || !publication) {
    return (
      <Box sx={{ minHeight: '100vh', bgcolor: '#fafbfc', py: 6 }}>
        <Container maxWidth="md">
          <Paper sx={{ p: 6, border: '1px solid #e1e4e8', boxShadow: 'none', borderRadius: 1, textAlign: 'center' }}>
            <Alert severity="error" sx={{ mb: 3, borderRadius: 1, border: '1px solid #d73a49' }}>{error || 'Publication not found'}</Alert>
            <Button 
              variant="outlined"
              startIcon={<ArrowBack />} 
              onClick={() => router.push('/community/publications')}
              sx={{ borderColor: '#d1d5da', color: '#24292e', '&:hover': { borderColor: '#24292e', bgcolor: '#f6f8fa' } }}>
              Back to Publications
            </Button>
          </Paper>
        </Container>
      </Box>
    );
  }

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: '#fafbfc', py: 8 }}>
      <Container maxWidth="xl">
        {/* Back Button - Minimal */}
        <Button 
          startIcon={<ArrowBack />} 
          onClick={() => router.push('/community/publications')} 
          size="small"
          sx={{ 
            mb: 4,
            color: '#586069',
            fontWeight: 400,
            textTransform: 'none',
            fontSize: '0.875rem',
            '&:hover': { bgcolor: 'transparent', color: '#0366d6' }
          }}>
          Back
        </Button>

        <Grid container spacing={4}>
          {/* Left Sidebar - Author & Tags */}
          <Grid size={{ xs: 12, md: 2 }}>
            <Box sx={{ position: { md: 'sticky' }, top: 24 }}>
              {/* Author Section */}
              <Paper sx={{ p: 3, mb: 3, border: '1px solid #e1e4e8', borderRadius: 1, boxShadow: 'none' }}>
                <Typography variant="caption" sx={{ color: '#586069', fontWeight: 600, textTransform: 'uppercase', letterSpacing: 0.5, mb: 2, display: 'block' }}>
                  Author
                </Typography>
                <Stack direction="column" spacing={1.5} alignItems="center" sx={{ textAlign: 'center' }}>
                  <Avatar sx={{ bgcolor: '#24292e', width: 56, height: 56, fontSize: '1.25rem' }}>
                    {publication.author.name.charAt(0).toUpperCase()}
                  </Avatar>
                  <Box>
                    <Typography variant="body2" sx={{ fontWeight: 600, color: '#24292e', mb: 0.5 }}>
                      {publication.author.name}
                    </Typography>
                    <Typography variant="caption" color="#586069">
                      {formatDate(publication.publishedAt || publication.createdAt)}
                    </Typography>
                  </Box>
                </Stack>
                {publication.readingTime && (
                  <Box sx={{ mt: 2, pt: 2, borderTop: '1px solid #e1e4e8', textAlign: 'center' }}>
                    <Typography variant="caption" color="#586069" sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 0.5 }}>
                      <AccessTime sx={{ fontSize: 14 }} />
                      {publication.readingTime} min read
                    </Typography>
                  </Box>
                )}
              </Paper>

              {/* Tags Section */}
              {publication.tags.length > 0 && (
                <Paper sx={{ p: 3, border: '1px solid #e1e4e8', borderRadius: 1, boxShadow: 'none' }}>
                  <Typography variant="caption" sx={{ color: '#586069', fontWeight: 600, textTransform: 'uppercase', letterSpacing: 0.5, mb: 2, display: 'block' }}>
                    Topics
                  </Typography>
                  <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                    {publication.tags.map((tag, idx) => (
                      <Chip 
                        key={idx} 
                        label={tag}
                        size="small"
                        sx={{ 
                          bgcolor: '#f6f8fa',
                          color: '#586069',
                          fontWeight: 500,
                          fontSize: '0.75rem',
                          border: '1px solid #e1e4e8',
                          height: 24,
                          '&:hover': { bgcolor: '#e1e4e8' }
                        }}
                      />
                    ))}
                  </Box>
                </Paper>
              )}

              {/* Share Section */}
              <Box sx={{ mt: 3, textAlign: 'center' }}>
                <IconButton 
                  onClick={handleShare}
                  sx={{ 
                    border: '1px solid #e1e4e8',
                    bgcolor: 'white',
                    '&:hover': { bgcolor: '#f6f8fa' }
                  }}>
                  <Share sx={{ fontSize: 18, color: '#586069' }} />
                </IconButton>
                <Typography variant="caption" display="block" sx={{ color: '#586069', mt: 1 }}>
                  Share
                </Typography>
              </Box>
            </Box>
          </Grid>

          {/* Main Content */}
          <Grid size={{ xs: 12, md: 10 }}>
            <Paper sx={{ border: '1px solid #e1e4e8', borderRadius: 1, boxShadow: 'none', overflow: 'hidden' }}>
              {/* Featured Image - Full Width */}
              {publication.featuredImage && (
                <Box sx={{ 
                  width: '100%', 
                  height: 350, 
                  backgroundImage: `url(${publication.featuredImage})`,
                  backgroundSize: 'cover', 
                  backgroundPosition: 'center',
                  borderBottom: '1px solid #e1e4e8'
                }} />
              )}

              <Box sx={{ p: { xs: 4, md: 6 } }}>
                {/* Title - Large, Clean */}
                <Typography variant="h3" sx={{ fontWeight: 600, lineHeight: 1.2, mb: 3, color: '#24292e', fontSize: { xs: '2rem', md: '2.5rem' } }}>
                  {publication.title}
                </Typography>

                {/* Description */}
                {publication.description && (
                  <Typography variant="body1" sx={{ color: '#586069', fontSize: '1.125rem', lineHeight: 1.6, mb: 4 }}>
                    {publication.description}
                  </Typography>
                )}

                <Divider sx={{ mb: 6 }} />

                {/* Content - Spacious, Clean Typography */}
                <Box sx={{ 
                  mb: 8,
                  '& h1': { fontSize: '1.875rem', fontWeight: 600, mb: 3, mt: 6, color: '#24292e', letterSpacing: '-0.02em' },
                  '& h2': { fontSize: '1.5rem', fontWeight: 600, mb: 2.5, mt: 5, color: '#24292e', letterSpacing: '-0.01em' },
                  '& h3': { fontSize: '1.25rem', fontWeight: 600, mb: 2, mt: 4, color: '#24292e' },
                  '& p': { mb: 3, lineHeight: 1.8, fontSize: '1.0625rem', color: '#24292e' },
                  '& ul, & ol': { mb: 3, pl: 4 },
                  '& li': { mb: 1, lineHeight: 1.8, fontSize: '1.0625rem', color: '#24292e' },
                  '& img': { maxWidth: '100%', height: 'auto', borderRadius: 1, my: 4 },
                  '& code': { 
                    bgcolor: '#f6f8fa', 
                    padding: '2px 6px', 
                    borderRadius: 0.5, 
                    fontSize: '0.9em',
                    fontFamily: 'SFMono-Regular, Consolas, monospace',
                    color: '#24292e'
                  },
                  '& pre': { 
                    bgcolor: '#f6f8fa', 
                    color: '#24292e',
                    p: 3, 
                    border: '1px solid #e1e4e8',
                    borderRadius: 1, 
                    overflow: 'auto',
                    my: 4,
                    '& code': {
                      bgcolor: 'transparent',
                      padding: 0
                    }
                  },
                  '& blockquote': {
                    borderLeft: '3px solid #e1e4e8',
                    pl: 3,
                    py: 1,
                    my: 3,
                    color: '#586069'
                  },
                  '& a': {
                    color: '#0366d6',
                    textDecoration: 'none',
                    borderBottom: '1px solid transparent',
                    '&:hover': {
                      borderBottomColor: '#0366d6'
                    }
                  },
                  '& table': {
                    border: '1px solid #e1e4e8',
                    borderCollapse: 'collapse',
                    width: '100%',
                    my: 3
                  },
                  '& th, & td': {
                    border: '1px solid #e1e4e8',
                    p: 2,
                    textAlign: 'left'
                  },
                  '& th': {
                    bgcolor: '#f6f8fa',
                    fontWeight: 600
                  }
                }}>
                  <MarkdownViewer content={publication.content} />
                </Box>

                {/* Engagement - Minimal */}
                <Stack direction="row" spacing={4} sx={{ pt: 6, borderTop: '1px solid #e1e4e8' }}>
                  <Button
                    onClick={handleLike}
                    startIcon={publication.isLikedByCurrentUser ? 
                      <Favorite sx={{ fontSize: 18, color: '#0366d6' }} /> : 
                      <FavoriteBorder sx={{ fontSize: 18, color: '#586069' }} />}
                    sx={{ 
                      color: publication.isLikedByCurrentUser ? '#0366d6' : '#586069',
                      textTransform: 'none',
                      fontWeight: 500,
                      '&:hover': { bgcolor: '#f6f8fa' }
                    }}>
                    {publication.likeCount} {publication.likeCount === 1 ? 'Like' : 'Likes'}
                  </Button>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, color: '#586069' }}>
                    <Visibility sx={{ fontSize: 18 }} />
                    <Typography variant="body2" fontWeight={500}>
                      {publication.viewCount} {publication.viewCount === 1 ? 'View' : 'Views'}
                    </Typography>
                  </Box>
                  <IconButton 
                    onClick={handleShare} 
                    size="small"
                    sx={{ color: '#586069', '&:hover': { bgcolor: '#f6f8fa' } }}>
                    <Share sx={{ fontSize: 18 }} />
                  </IconButton>
                </Stack>
              </Box>
            </Paper>
          </Grid>
        </Grid>
      </Container>
    </Box>
  );
}