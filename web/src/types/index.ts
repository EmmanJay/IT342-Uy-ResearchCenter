// User and Authentication Types
export interface User {
  id: string;
  email: string;
  firstname: string;
  lastname: string;
  role: 'ADMIN' | 'USER';
}

export interface AuthData {
  user: User;
  token: string;
  refreshToken?: string;
}

// API Response Wrapper
export interface ApiResponse<T> {
  data: T;
  message: string;
  success: boolean;
}

// Repository Types
export type RepositoryStatusType = 'TO_READ' | 'IN_PROGRESS' | 'COMPLETED';

export const RepositoryStatus = {
  TO_READ: 'TO_READ' as const,
  IN_PROGRESS: 'IN_PROGRESS' as const,
  COMPLETED: 'COMPLETED' as const,
};

export interface RepositoryMember {
  id: string;
  userId: string;
  firstname?: string;
  lastname?: string;
  email?: string;
  roleInRepo: 'OWNER' | 'MEMBER';
  addedAt?: string;
}

export interface Repository {
  id: string;
  name: string;
  description: string;
  ownerId: string;
  ownerName?: string;
  createdAt: string;
  updatedAt: string;
}

export interface RepositoryDetail extends Repository {
  members: RepositoryMember[];
  materialCount?: number;
  requestCount?: number;
}

// Material Types
export type MaterialTypeValue = 'PDF' | 'LINK' | 'REFERENCE';

export const MaterialType = {
  PDF: 'PDF' as const,
  LINK: 'LINK' as const,
  REFERENCE: 'REFERENCE' as const,
};

export type MaterialStatusValue = 'TO_READ' | 'IN_PROGRESS' | 'COMPLETED';

export const MaterialStatus = {
  TO_READ: 'TO_READ' as const,
  IN_PROGRESS: 'IN_PROGRESS' as const,
  COMPLETED: 'COMPLETED' as const,
};

export interface MaterialTag {
  id: string;
  tag: string;
}

export interface GoogleBookMetadata {
  isbn?: string;
  title?: string;
  authors?: string[];
  imageUrl?: string;
  publishedDate?: string;
  publisher?: string;
  description?: string;
}

export interface Material {
  id: string;
  title: string;
  description?: string;
  type: MaterialTypeValue;
  repositoryId: string;
  uploadedBy: string;
  uploadedByName?: string;
  status: MaterialStatusValue;
  tags: MaterialTag[];
  metadata?: GoogleBookMetadata;
  fileUrl?: string; // for PDF
  url?: string; // for LINK
  isbn?: string;
  createdAt: string;
  updatedAt: string;
}

// Request Types
export type MaterialRequestStatusValue = 'OPEN' | 'PENDING' | 'FULFILLED' | 'CANCELLED';

export const MaterialRequestStatus = {
  OPEN: 'OPEN' as const,
  PENDING: 'PENDING' as const,
  FULFILLED: 'FULFILLED' as const,
  CANCELLED: 'CANCELLED' as const,
};

export interface MaterialRequest {
  id: string;
  title: string;
  description: string;
  status: MaterialRequestStatusValue;
  repositoryId: string;
  requesterId?: string;
  requesterName?: string;
  materialId?: string; // when fulfilled
  fulfilledBy?: string; // user who fulfilled the request
  fulfilledByName?: string;
  fulfilledAt?: string;
  createdAt: string;
  updatedAt: string;
}

// Form DTOs
export interface CreateRepositoryRequest {
  name: string;
  description: string;
}

export interface CreateMaterialRequest {
  title: string;
  type: MaterialTypeValue;
  description?: string;
  repositoryId: string;
  status?: MaterialStatusValue;
  tags?: string[];
  fileUrl?: string; // for PDF from Supabase
  url?: string; // for LINK
  metadata?: GoogleBookMetadata;
  isbn?: string;
  publisher?: string;
  year?: string;
  authors?: string[] | string;
}

export interface CreateRequestDto {
  title: string;
  description: string;
  repositoryId: string;
}

export interface FulfillRequestDto {
  materialId: string;
}

export interface InviteMemberRequest {
  email: string;
}
