import { Project, ProjectStatus } from '@/types/api';
import { httpClient } from '@/utils/api/http-client';

const API_BASE_URL = '/api/thematician/projects';

export interface ProjectSharingRequest {
    projectId: string;
    otherEmail: string;
}

export const fetchStatistics = async () => {
    try {
        const response = await httpClient.get(`${API_BASE_URL}/statistics`, {
            requiresAuth: true
        });
        return response.data as {
            totalProjects: number;
            imagesPerProject: Record<string, number>;
            lastAccessTime?: Record<string, string>;
        };
    } catch (error) {
        console.error('Error fetching statistics:', error);
        throw new Error('Failed to fetch statistics');
    }
};

export interface PaginatedResponse<T> {
    content: T[];
    pageNumber: number;
    pageSize: number;
    totalElements: number;
    totalPages: number;
    first: boolean;
    last: boolean;
    empty: boolean;
}

export interface ProjectsResponse {
    timestamp: string;
    status: string;
    message: string;
    data: PaginatedResponse<Project>;
}

export const getAllProjects = async (page: number = 0, size: number = 10): Promise<PaginatedResponse<Project>> => {
    try {
        const response = await httpClient.get(`${API_BASE_URL}/all?page=${page}&size=${size}`, {
            requiresAuth: true
        });
        return response.data;
    } catch (error) {
        console.error('Error fetching projects:', error);
        throw new Error('Failed to fetch projects');
    }
};

export const getProject = async (id: string): Promise<Project> => {
    try {
        const response = await httpClient.get(`${API_BASE_URL}/${id}`, {
            requiresAuth: true
        });
        return response.data;
    } catch (error) {
        console.error('Error fetching project:', error);
        throw new Error('Failed to fetch project');
    }
};

export const shareProject = async (request: ProjectSharingRequest): Promise<void> => {
    try {
        await httpClient.post(`${API_BASE_URL}/${request.projectId}/share`, 
            { email: request.otherEmail },
            { requiresAuth: true }
        );
    } catch (error) {
        console.error('Error sharing project:', error);
        throw new Error('Failed to share project');
    }
};

export const unshareProject = async (request: ProjectSharingRequest): Promise<void> => {
    try {
        await httpClient.post(
            `${API_BASE_URL}/${request.projectId}/unshare`,
            { email: request.otherEmail },
            { requiresAuth: true }
        );
    } catch (error) {
        console.error('Error unsharing project:', error);
        throw new Error('Failed to unshare project');
    }
};

export const archiveProject = async (id: string): Promise<void> => {
    try {
        await httpClient.post(
            `${API_BASE_URL}/${id}/archive`,
            {},
            { requiresAuth: true }
        );
    } catch (error) {
        console.error('Error archiving project:', error);
        throw new Error('Failed to archive project');
    }
};

export const unarchiveProject = async (id: string): Promise<void> => {
    try {
        await httpClient.post(
            `${API_BASE_URL}/${id}/unarchive`,
            {},
            { requiresAuth: true }
        );
    } catch (error) {
        console.error('Error unarchiving project:', error);
        throw new Error('Failed to unarchive project');
    }
};

export const deleteProject = async (id: string): Promise<void> => {
    try {
        await httpClient.delete(`${API_BASE_URL}/${id}`, {
            requiresAuth: true
        });
    } catch (error) {   
        console.error('Error deleting project:', error);
        throw new Error('Failed to delete project');
    }
};
export const createProject = async (projectData: {
    projectName: string;
    description?: string;
    tags?: string[];
    status?: string;
}): Promise<Project> => {
    try {
        const response = await httpClient.post(`${API_BASE_URL}/create`, {
            projectName: projectData.projectName,
            description: projectData.description || '',
            tags: projectData.tags || ['initial'],
            status: projectData.status || 'CREATED'
        }, {
            requiresAuth: true
        });

        return response.data;
    } catch (error) {
        console.error('Error creating project:', error);
        throw new Error('Failed to create project');
    }
};

// Ensure createProject is exported in projectsService
export const projectsService = {
    fetchStatistics,
    getAllProjects,
    getProject,
    createProject,
    shareProject,
    unshareProject,
    archiveProject,
    unarchiveProject,
    deleteProject
};
