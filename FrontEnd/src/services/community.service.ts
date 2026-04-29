import { httpClient } from '@/utils/api/http-client';
import { User } from '@/types/user';

export const getCommunityUsers = async (): Promise<User[]> => {
  const response = await httpClient.get('/api/community/users');
  return response.data || response;
};

export const communityService = {
  getCommunityUsers,
};
