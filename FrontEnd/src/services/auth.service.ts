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

      // Store the JWT token - Access directly from the response object
      const accessToken = response?.accessToken;
      if (accessToken) {
        localStorage.setItem('token', accessToken);
        
        // Backend returns id, username and email in the response
        const user = {
          id: response.id, // User's ObjectId from backend
          username: response.username || response.email, // The actual username from registration
          email: response.email,
          roles: response.roles || []
        };
        
        console.log('[AuthService] Login response:', response);
        console.log('[AuthService] Storing user:', user);
        
        localStorage.setItem('user', JSON.stringify(user));
        localStorage.setItem('userRoles', JSON.stringify(response.roles || []));
        
        return response;
      } else {
        throw new Error('No token received from server');
      }

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
      
      const accessToken = response.data?.accessToken || response.accessToken;
      if (accessToken) {
        localStorage.setItem('token', accessToken);

        // Store additional user information if available
        if (response.data?.roles) {
          localStorage.setItem('userRoles', JSON.stringify(response.data.roles));
        }
        if (response.data?.username) {
          localStorage.setItem('username', response.data.username);
        }
        if (response.data?.email) {
          localStorage.setItem('email', response.data.email);
        }
      }
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
    // Check if running in a browser environment
    if (typeof window !== 'undefined') {
      return localStorage.getItem('token');
    }
    return null; // Return null if not in a browser (e.g., SSR)
  }

  isAuthenticated(): boolean {
    // Check if running in a browser environment before getting token
    if (typeof window === 'undefined') {
      return false; // Cannot be authenticated on the server without a token check mechanism
    }
    const token = this.getToken();
    return !!token;
  }

  logout() {
    // Check for window existence before accessing localStorage and window
    if (typeof window !== 'undefined') {
        localStorage.removeItem('token');
        localStorage.removeItem('user'); // Remove the stored user object
        // Clear legacy items if they exist
        localStorage.removeItem('userRoles');
        localStorage.removeItem('username');
        localStorage.removeItem('email');
        window.location.href = '/auth/login';
    }
  }
}

export const authService = new AuthService()
