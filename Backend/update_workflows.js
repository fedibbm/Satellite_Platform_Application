const fs = require('fs');
const filePath = '../FrontEnd/src/app/workflows/page.tsx';

let content = fs.readFileSync(filePath, 'utf8');

if (!content.includes("import { getAllProjects }")) {
    content = content.replace("export default function WorkflowsPage() {", "import { getAllProjects } from '@/services/projects.service';\n\nexport default function WorkflowsPage() {");
}

const newLoadData = `  const loadData = async () => {
    try {
      setLoading(true);
      const [ownedWorkflowsData, templatesData] = await Promise.all([
        workflowService.getAllWorkflows(),
        workflowService.getWorkflowTemplates(),
      ]);
      
      let sharedWorkflows = [];
      try {
        const projectsResponse = await getAllProjects(0, 100);
        if (projectsResponse?.content && projectsResponse.content.length > 0) {
          const projectPromises = projectsResponse.content.map(p => {
             const pId = p.id || (p as any)._id;
             if (!pId) return Promise.resolve([]);
             return workflowService.getWorkflowsByProject(pId).catch(() => []);
          });
          const results = await Promise.all(projectPromises);
          sharedWorkflows = results.flat();
        }
      } catch (err) {
        console.warn("Could not fetch project workflows", err);
      }
      
      const allWfs = [...(ownedWorkflowsData || []), ...sharedWorkflows];
      const uniqueWfs = Array.from(new Map(allWfs.map(item => [item.id, item])).values());

      setWorkflows(uniqueWfs);
      setTemplates(templatesData || []);
    } catch (error) {
      console.error('Error loading workflows:', error);
      setWorkflows([]);
      setTemplates([]);
    } finally {
      setLoading(false);
    }
  };`;

content = content.replace(/const loadData = async \(\) => \{[\s\S]*?finally \{\s*setLoading\(false\);\s*\}\s*\};\s*const handleCreateWorkflow = \(\) => \{/m, newLoadData + "\n\n  const handleCreateWorkflow = () => {");

fs.writeFileSync(filePath, content);
