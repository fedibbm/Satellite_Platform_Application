# Debug LocalStorage Username Issue

## Quick Check - Open Browser Console (F12) and run:

```javascript
// Check what's stored
console.log('user object:', localStorage.getItem('user'));
console.log('userId:', localStorage.getItem('userId'));
console.log('username:', localStorage.getItem('username'));
console.log('email:', localStorage.getItem('email'));

// Parse user object
try {
  const user = JSON.parse(localStorage.getItem('user'));
  console.log('Parsed user:', user);
  console.log('Username from user object:', user?.username);
  console.log('ID from user object:', user?.id);
} catch (e) {
  console.log('No valid user object');
}
```

## What Should Happen Now:

### ✅ Message Order Fixed:
- Messages now sorted by timestamp (oldest → newest)
- New messages appear at the **bottom** (not top)
- Auto-scroll to latest message works correctly

### ✅ Username Display Fixed:
The `getCurrentUserName()` function now:
1. First tries to get username from `user` object in localStorage
2. Falls back to direct keys: `username`, `userName`, `name`, `email`
3. Returns 'User' as last resort

The `getCurrentUserId()` function now:
1. First tries to get ID from `user` object in localStorage
2. Falls back to `userId` or `email`

## If Username Still Shows "User":

After logging in, the user object should be stored. Check console for:
```
[useMessaging] User object from localStorage: {id: "...", username: "...", email: "..."}
```

If you see `null` or undefined, the issue is in the login process not storing the data properly.

## Manual Fix (if needed):

If after login you still see "User", manually set in console:
```javascript
// Get your current user data from the login response
const userData = {
  id: "YOUR_USER_ID",
  username: "YourUsername", 
  email: "your@email.com",
  roles: ["ROLE_THEMATICIAN"]
};
localStorage.setItem('user', JSON.stringify(userData));

// Then refresh the page
location.reload();
```
