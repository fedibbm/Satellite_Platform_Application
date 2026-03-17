const fs = require('fs');

const filePath = '../FrontEnd/src/components/Header.tsx';
let content = fs.readFileSync(filePath, 'utf8');

// Ensure AccountTree icon is imported for workflows
if (!content.includes('AccountTreeIcon')) {
  content = content.replace(
    "import StorageIcon from '@mui/icons-material/Storage';",
    "import StorageIcon from '@mui/icons-material/Storage';\nimport AccountTreeIcon from '@mui/icons-material/AccountTree';"
  );
}

// Add the Workflow link to workspaceLinks
const workspaceLinksRegex = /const workspaceLinks = \[\s*\{ name: 'Home', href: '\/', icon: HomeIcon \},\s*\{ name: 'Dashboard', href: '\/dashboard', icon: DashboardIcon \},\s*\{ name: 'Projects', href: '\/projects', icon: FolderIcon \},\s*\{ name: 'Analysis', href: '\/analysis', icon: AnalyticsIcon \},\s*\{ name: 'Storage', href: '\/storage', icon: StorageIcon \},?\s*\];/m;

const updatedWorkspaceLinks = `const workspaceLinks = [
    { name: 'Home', href: '/', icon: HomeIcon },
    { name: 'Dashboard', href: '/dashboard', icon: DashboardIcon },
    { name: 'Projects', href: '/projects', icon: FolderIcon },
    { name: 'Workflows', href: '/workflows', icon: AccountTreeIcon },
    { name: 'Analysis', href: '/analysis', icon: AnalyticsIcon },
    { name: 'Storage', href: '/storage', icon: StorageIcon },
  ];`;

content = content.replace(workspaceLinksRegex, updatedWorkspaceLinks);

fs.writeFileSync(filePath, content);
console.log('Updated Header.tsx successfully.');
