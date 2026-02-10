import { AUTH_ENDPOINTS } from '../config/api'
import { httpClient } from '../utils/api/http-client'
import { Authority } from '@/types/auth'

interface LoginCredentials {
  username: string
  password: string
}

interface RegisterData {
  username: string
  email: string
  password: string
  roles: string[]
}

class AuthService {
  private loginAttempts = 0;
  private readonly maxLoginAttempts = 3;
  private loginCooldownEnd = 0;
  private readonly loginCooldownDuration = 60000; // 1 minute cooldown

  async login(credentials: LoginCredentials) {
    const now = Date.now();
    
    // Check if we're in a cooldown period
    if (now < this.loginCooldownEnd) {
      const remainingSeconds = Math.ceil((this.loginCooldownEnd - now) / 1000);
      throw new Error(`Too many login attempts. Please wait ${remainingSeconds} seconds before trying again.`);
    }

    try {
      const response = await httpClient.post(AUTH_ENDPOINTS.LOGIN, credentials, {
        requiresAuth: false,
      });
      
      // Reset login attempts on successful login
      this.loginAttempts = 0;
      this.loginCooldownEnd = 0;

      // Tokens are now in HTTP-only cookies, just store user info
      const user = {
        id: response.id,
        username: response.username || response.email,
        email: response.email,
        roles: response.roles || []
      };
      
      console.log('[AuthService] Login successful!');
      console.log('[AuthService] Response:', response);
      console.log('[AuthService] Storing user:', user);
      console.log('[AuthService] Cookies should be set by browser automatically');
      
      localStorage.setItem('user', JSON.stringify(user));
      localStorage.setItem('userRoles', JSON.stringify(response.roles || []));
      
      console.log('[AuthService] User stored in localStorage');
      console.log('[AuthService] Check Application > Cookies > localhost:8080 for accessToken');
      
      return response;
    } catch (error: any) {
      // Handle rate limiting specifically
      if (error.message.includes('Rate limit')) {
        this.loginAttempts++;
        
        // If we've exceeded max attempts, enforce a cooldown
        if (this.loginAttempts >= this.maxLoginAttempts) {
          this.loginCooldownEnd = now + this.loginCooldownDuration;
          throw new Error(`Too many login attempts. Please wait 60 seconds before trying again.`);
        }
        
        throw new Error('Login temporarily unavailable. Please try again in a few seconds.');
      }
      
      console.error('Login error:', error);
      throw new Error(error.message || 'Login failed');
    }
  }

  async register(email: string, password: string, username: string, roles?: string[]): Promise<void> {
    try {
      const response = await httpClient.post(AUTH_ENDPOINTS.REGISTER, {
        username,
        email,
        password,
        roles: roles || ['THEMATICIAN']  // Backend expects roles as array, default to THEMATICIAN if not provided
      }, { requiresAuth: false });
      
    } catch (error) {
      console.error('Registration failed:', error);
      throw error;
    }
  }

  async resetPassword(email: string) {
    try {
      const response = await httpClient.post(
        AUTH_ENDPOINTS.RESET_PASSWORD,
        { email },
        { requiresAuth: false }
      )
      return response
    } catch (error: any) {
      throw new Error(error.message || 'Password reset request failed')
    }
  }

  async getAvailableRoles(): Promise<Authority[]> {
    try {
      const response = await httpClient.get(AUTH_ENDPOINTS.GET_ROLES, {
        requiresAuth: false,
      });
      // Response structure: { status: 'SUCCESS', message: '...', data: [...] }
      return response.data || response;
    } catch (error: any) {
      console.error('Failed to fetch roles:', error);
      throw new Error(error.message || 'Failed to fetch available roles');
    }
  }

  getToken(): string | null {
    // Tokens are now in HTTP-only cookies, not accessible via JavaScript
    // This method is kept for backwards compatibility but returns null
    // Authentication is handled by cookies sent automatically with requests
    return null;
  }

  isAuthenticated(): boolean {
    // Check if user info exists in localStorage
    // The actual auth is validated by the backend via cookies
    if (typeof window === 'undefined') {
      return false;
    }
    const user = localStorage.getItem('user');
    return !!user;
  }

  async logout() {
    // Check for window existence before accessing localStorage and window
    if (typeof window !== 'undefined') {
        // Call backend logout endpoint to clear cookies
        try {
          await httpClient.post(AUTH_ENDPOINTS.LOGOUT, {}, {
            requiresAuth: true,
          });
        } catch (error) {
          console.error('Logout error:', error);
          // Continue with local cleanup even if backend call fails
        }
        
        // Clear local storage
        localStorage.removeItem('user');
        localStorage.removeItem('userRoles');
        localStorage.removeItem('username');
        localStorage.removeItem('email');
        localStorage.removeItem('token'); // Remove legacy token if exists
        
        window.location.href = '/auth/login';
    }
  }
}

export const authService = new AuthService()
