'use client';

import { Box, Container, Typography, Paper } from '@mui/material';

export default function ForumsPage() {
  return (
    <Box
      sx={{
        minHeight: '100vh',
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        py: 4,
      }}
    >
      <Container maxWidth="xl">
        <Typography
          variant="h3"
          sx={{
            color: 'white',
            fontWeight: 700,
            mb: 3,
            textShadow: '0 2px 4px rgba(0,0,0,0.1)',
          }}
        >
          Community Forums
        </Typography>

        <Paper sx={{ p: 4, textAlign: 'center' }}>
          <Typography variant="h6" color="text.secondary" gutterBottom>
            Forums coming soon!
          </Typography>
          <Typography variant="body1" color="text.secondary">
            Discuss satellite imagery analysis, share insights, and ask questions.
          </Typography>
        </Paper>
      </Container>
    </Box>
  );
}
