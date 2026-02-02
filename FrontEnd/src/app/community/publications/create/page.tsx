'use client';

import { useState } from 'react';
import { Box, Container, Typography, Alert, CircularProgress } from '@mui/material';
import { useRouter } from 'next/navigation';
import PublicationEditor from '@/components/PublicationEditor';
import publicationService from '@/services/publicationService';

interface PublicationMetadata {
  title: string;
  tags: string[];
  description: string;
}

export default function CreatePublicationPage() {
  const router = useRouter();
  const [showSuccess, setShowSuccess] = useState(false);
  const [showError, setShowError] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSave = async (content: string, metadata: PublicationMetadata) => {
    setIsSubmitting(true);
    setShowError(false);
    setShowSuccess(false);

    try {
      // Validate required fields
      if (!metadata.title || metadata.title.trim().length < 3) {
        throw new Error('Title must be at least 3 characters long');
      }

      if (!content || content.trim().length < 10) {
        throw new Error('Content must be at least 10 characters long');
      }

      if (metadata.description && metadata.description.length > 500) {
        throw new Error('Description must not exceed 500 characters');
      }

      // Calculate reading time (rough estimate: 200 words per minute)
      const wordCount = content.split(/\s+/).filter(word => word.length > 0).length;
      const readingTime = Math.max(1, Math.ceil(wordCount / 200));

      const publicationData = {
        title: metadata.title.trim(),
        description: metadata.description?.trim() || '',
        tags: metadata.tags || [],
        content: content,
        status: 'PUBLISHED' as const,
        readingTime: readingTime,
      };

      console.log('Sending publication data:', publicationData);

      await publicationService.createPublication(publicationData);

      // Show success message
      setShowSuccess(true);
      setTimeout(() => {
        router.push('/community/publications');
      }, 1500);
    } catch (error: any) {
      console.error('Error creating publication:', error);
      setErrorMessage(error.message || 'Failed to create publication. Please try again.');
      setShowError(true);
      setIsSubmitting(false);
    }
  };

  return (
    <Box
      sx={{
        minHeight: '100vh',
        background: '#fafafa',
        py: 4,
      }}
    >
      <Container maxWidth="xl">
        <Box sx={{ mb: 4 }}>
          <Typography
            variant="h4"
            sx={{
              color: '#1a1a1a',
              fontWeight: 700,
              mb: 1,
            }}
          >
            Create New Publication
          </Typography>
          <Typography
            variant="body1"
            sx={{
              color: '#666',
              fontSize: '1rem',
            }}
          >
            Share your satellite imagery research and analysis with the community
          </Typography>
        </Box>

        {showSuccess && (
          <Alert severity="success" sx={{ mb: 3 }}>
            Publication created successfully! Redirecting...
          </Alert>
        )}

        {showError && (
          <Alert severity="error" sx={{ mb: 3 }} onClose={() => setShowError(false)}>
            {errorMessage}
          </Alert>
        )}

        {isSubmitting && (
          <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', mb: 3, gap: 2 }}>
            <CircularProgress size={24} />
            <Typography>Creating publication...</Typography>
          </Box>
        )}

        <PublicationEditor onSave={handleSave} isDisabled={isSubmitting} />
      </Container>
    </Box>
  );
}
