"use client"; 

import {
    useState,
    useEffect,
    useCallback,
    useContext,
    createContext,
} from "react";
import { authService } from "@/services/auth.service";

interface User {
    id: string;
    username: string;
    email: string;
    roles: string[];
    // Add other relevant fields if needed (e.g., enabled, accountLocked etc. if returned by login)
}

interface AuthContextType {
    user: User | null;
    token: string | null;
    loading: boolean;
    login: (credentials: any) => Promise<void>; // Adjust credentials type as needed
    logout: () => void;
    isLoggedIn: () =>boolean;
}

// Create a context with a default undefined value to check if provider is used
const AuthContext = createContext<AuthContextType | undefined>(undefined);

// AuthProvider component to wrap the application
export function AuthProvider({ children }: { children: React.ReactNode }) {
    const [user, setUser] = useState<User | null>(null);
    const [token, setToken] = useState<string | null>(null);
    const [loading, setLoading] = useState(true); // Start loading until initial check is done

    // Check localStorage on initial mount (client-side only)
    useEffect(() => {
        try {
            // With HTTP-only cookies, we don't store tokens in localStorage anymore
            // Just check for user object
            const storedUser = localStorage.getItem("user");

            console.log("[useAuth] Initial check - User exists:", !!storedUser);

            if (storedUser) {
                // User exists, parse and set it
                try {
                    const parsedUser: User = JSON.parse(storedUser);
                    console.log("[useAuth] Parsed user:", parsedUser);

                    // Basic validation - check if it has at least an id
                    if (parsedUser && parsedUser.id) {
                        setUser(parsedUser);
                        setToken("cookie-based-auth"); // Set dummy token to indicate authenticated
                    } else {
                        console.warn(
                            "[useAuth] User object missing id field, clearing user"
                        );
                        localStorage.removeItem("user");
                        setUser(null);
                        setToken(null);
                    }
                } catch (parseError) {
                    console.error(
                        "[useAuth] Failed to parse user JSON:",
                        parseError
                    );
                    localStorage.removeItem("user");
                    setUser(null);
                    setToken(null);
                }
            } else {
                console.log("[useAuth] No user found - not authenticated");
                setToken(null);
                setUser(null);
            }
        } catch (error) {
            console.error(
                "[useAuth] Error reading auth state from localStorage:",
                error
            );
            // Only clear on catastrophic error
            setToken(null);
            setUser(null);
        } finally {
            setLoading(false);
        }
    }, []); // Run only once on mount

    // Login function
    const login = useCallback(async (credentials: any) => {
        setLoading(true);
        try {
            const response = await authService.login(credentials);
            const receivedToken = response?.accessToken; // Adjust based on actual response structure
            const receivedUser = response; // Assuming response is the user object
            
            if (!receivedToken || !receivedUser?.id || !receivedUser?.username) {
              localStorage.removeItem("token");
              localStorage.removeItem("user");
              setToken(null);
              setUser(null);
                console.error(
                    "Login successful but token or user data missing in response."
                );
              throw new Error("Error: Incomplete Data received");
            }

            const userToStore: User = {
                id: receivedUser.id,
                username: receivedUser.username,
                email: receivedUser.email,
                roles: receivedUser.roles,
            };
            setToken(receivedToken);
            setUser(userToStore);
            localStorage.setItem("token", receivedToken);
            localStorage.setItem("user", JSON.stringify(userToStore));

            
        } catch (error) {
            // Clear state and storage on login failure
            localStorage.removeItem("token");
            localStorage.removeItem("user");
            setToken(null);
            setUser(null);
            console.error("Login failed:", error);
            throw error; // Re-throw error to be caught by the calling component
        } finally {
            setLoading(false);
        }
    }, []); // No dependencies needed as authService is stable

    // Logout function
    const logout = useCallback(() => {
        setLoading(true); // Indicate activity
        try {
            authService.logout(); // This handles localStorage clearing and redirect
            // Clear state immediately
            setUser(null);
            setToken(null);
        } catch (error) {
            console.error("Error during logout:", error);
        } finally {
            // setLoading(false); // No need to set loading false as page will redirect
        }
        // Note: authService.logout() performs the redirect, so state updates
        // here are mainly for immediate UI feedback before navigation.
    }, []); // No dependencies needed
      const isLoggedIn = useCallback(()=>{
        return authService.isAuthenticated();
      },[user,token])

    const value = { user, token, loading, login, logout, isLoggedIn };

    return (
        <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
    );
}

// Custom hook to use the AuthContext
export function useAuth() {
    const context = useContext(AuthContext);
    if (context === undefined) {
        throw new Error("useAuth must be used within an AuthProvider");
    }
    return context;
}
