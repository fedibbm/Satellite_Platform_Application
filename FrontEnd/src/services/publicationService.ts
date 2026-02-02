import { httpClient } from '../utils/api/http-client';

export interface PublicationAuthor {
  id: string;
  name: string;
  email: string;
}

export interface Publication {
  id: string;
  author: PublicationAuthor;
  title: string;
  description: string;
  content: string;
  tags: string[];
  status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
  viewCount: number;
  likeCount: number;
  commentCount: number;
  featuredImage?: string;
  readingTime?: number;
  createdAt: string;
  updatedAt: string;
  publishedAt?: string;
  isLikedByCurrentUser: boolean;
}

export interface CreatePublicationDto {
  title: string;
  description?: string;
  content: string;
  tags?: string[];
  status?: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
  featuredImage?: string;
  readingTime?: number;
}

export interface UpdatePublicationDto {
  title?: string;
  description?: string;
  content?: string;
  tags?: string[];
  status?: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
  featuredImage?: string;
  readingTime?: number;
}

export interface PublicationPageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

const BASE_URL = '/api/community/publications';

class PublicationService {
  async createPublication(data: CreatePublicationDto): Promise<Publication> {
    const response = await httpClient.post<{ status: string; message: string; data: Publication }>(
      BASE_URL,
      data
    );
    return response.data;
  }

  async getAllPublications(params?: {
    page?: number;
    size?: number;
    sortBy?: string;
    sortDir?: 'ASC' | 'DESC';
  }): Promise<PublicationPageResponse<Publication>> {
    const queryParams = new URLSearchParams();
    if (params?.page !== undefined) queryParams.append('page', params.page.toString());
    if (params?.size !== undefined) queryParams.append('size', params.size.toString());
    if (params?.sortBy) queryParams.append('sortBy', params.sortBy);
    if (params?.sortDir) queryParams.append('sortDir', params.sortDir);
    
    const url = queryParams.toString() ? `${BASE_URL}?${queryParams}` : BASE_URL;
    const response = await httpClient.get<{ status: string; message: string; data: PublicationPageResponse<Publication> }>(url);
    return response.data;
  }

  async getPublicationById(id: string): Promise<Publication> {
    const response = await httpClient.get<{ status: string; message: string; data: Publication }>(
      `${BASE_URL}/${id}`
    );
    return response.data;
  }

  async updatePublication(id: string, data: UpdatePublicationDto): Promise<Publication> {
    const response = await httpClient.put<{ status: string; message: string; data: Publication }>(
      `${BASE_URL}/${id}`,
      data
    );
    return response.data;
  }

  async deletePublication(id: string): Promise<void> {
    await httpClient.delete(`${BASE_URL}/${id}`);
  }

  async toggleLike(id: string): Promise<Publication> {
    const response = await httpClient.post<{ status: string; message: string; data: Publication }>(
      `${BASE_URL}/${id}/like`
    );
    return response.data;
  }

  async searchPublications(params: {
    query: string;
    page?: number;
    size?: number;
  }): Promise<PublicationPageResponse<Publication>> {
    const queryParams = new URLSearchParams();
    queryParams.append('query', params.query);
    if (params.page !== undefined) queryParams.append('page', params.page.toString());
    if (params.size !== undefined) queryParams.append('size', params.size.toString());
    
    const response = await httpClient.get<{ status: string; message: string; data: PublicationPageResponse<Publication> }>(
      `${BASE_URL}/search?${queryParams}`
    );
    return response.data;
  }

  async getPublicationsByTag(params: {
    tag: string;
    page?: number;
    size?: number;
  }): Promise<PublicationPageResponse<Publication>> {
    const { tag, ...rest } = params;
    const queryParams = new URLSearchParams();
    if (rest.page !== undefined) queryParams.append('page', rest.page.toString());
    if (rest.size !== undefined) queryParams.append('size', rest.size.toString());
    
    const url = queryParams.toString() 
      ? `${BASE_URL}/tag/${encodeURIComponent(tag)}?${queryParams}`
      : `${BASE_URL}/tag/${encodeURIComponent(tag)}`;
    const response = await httpClient.get<{ status: string; message: string; data: PublicationPageResponse<Publication> }>(url);
    return response.data;
  }

  async getTrendingPublications(params?: {
    page?: number;
    size?: number;
  }): Promise<PublicationPageResponse<Publication>> {
    const queryParams = new URLSearchParams();
    if (params?.page !== undefined) queryParams.append('page', params.page.toString());
    if (params?.size !== undefined) queryParams.append('size', params.size.toString());
    
    const url = queryParams.toString() ? `${BASE_URL}/trending?${queryParams}` : `${BASE_URL}/trending`;
    const response = await httpClient.get<{ status: string; message: string; data: PublicationPageResponse<Publication> }>(url);
    return response.data;
  }

  async getPublicationsByAuthor(params: {
    email: string;
    page?: number;
    size?: number;
  }): Promise<PublicationPageResponse<Publication>> {
    const { email, ...rest } = params;
    const queryParams = new URLSearchParams();
    if (rest.page !== undefined) queryParams.append('page', rest.page.toString());
    if (rest.size !== undefined) queryParams.append('size', rest.size.toString());
    
    const url = queryParams.toString()
      ? `${BASE_URL}/author/${encodeURIComponent(email)}?${queryParams}`
      : `${BASE_URL}/author/${encodeURIComponent(email)}`;
    const response = await httpClient.get<{ status: string; message: string; data: PublicationPageResponse<Publication> }>(url);
    return response.data;
  }

  async getMyPublications(params?: {
    status?: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
    page?: number;
    size?: number;
  }): Promise<PublicationPageResponse<Publication>> {
    const queryParams = new URLSearchParams();
    if (params?.status) queryParams.append('status', params.status);
    if (params?.page !== undefined) queryParams.append('page', params.page.toString());
    if (params?.size !== undefined) queryParams.append('size', params.size.toString());
    
    const url = queryParams.toString() ? `${BASE_URL}/my?${queryParams}` : `${BASE_URL}/my`;
    const response = await httpClient.get<{ status: string; message: string; data: PublicationPageResponse<Publication> }>(url);
    return response.data;
  }
}

const publicationService = new PublicationService();
export default publicationService;
