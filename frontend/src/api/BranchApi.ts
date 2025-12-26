import { apiClient } from "./ApiClient";

export interface BranchShiftTemplate {
  id?: number;
  shiftType: "MORNING" | "EVENING";
  startTime: string; // HH:mm
  endTime: string;   // HH:mm
  shiftHours?: number;
}

export interface Branch {
  id: number;
  name: string;
  address: string;
  city: string;
  phone?: string;
  timezone: string;
  active: boolean;
  shiftTemplates: BranchShiftTemplate[];
}

export interface CreateBranchDto {
  name: string;
  address: string;
  city: string;
  phone?: string;
  timezone?: string;
}

export interface UpdateBranchDto {
  name?: string;
  address?: string;
  city?: string;
  phone?: string;
  timezone?: string;
  active?: boolean;
}

export interface CreateShiftTemplateDto {
  shiftType: "MORNING" | "EVENING";
  startTime: string;
  endTime: string;
}

const BranchApi = {
  /**
   * Get all branches (optionally filter active only)
   */
  async getAll(activeOnly = false): Promise<Branch[]> {
    const url = activeOnly ? "/branches/active" : "/branches";
    const response = await apiClient.get<Branch[]>(url);
    return response.data;
  },

  /**
   * Get a single branch by ID
   */
  async getById(branchId: number): Promise<Branch> {
    const response = await apiClient.get<Branch>(`/branches/${branchId}`);
    return response.data;
  },

  /**
   * Create a new branch
   */
  async create(branch: CreateBranchDto): Promise<Branch> {
    const response = await apiClient.post<Branch>("/branches", branch);
    return response.data;
  },

  /**
   * Update an existing branch
   */
  async update(branchId: number, updates: UpdateBranchDto): Promise<Branch> {
    const response = await apiClient.put<Branch>(`/branches/${branchId}`, updates);
    return response.data;
  },

  /**
   * Add a shift template to a branch
   */
  async addShiftTemplate(branchId: number, template: CreateShiftTemplateDto): Promise<Branch> {
    const response = await apiClient.post<Branch>(
      `/branches/${branchId}/shift-templates`,
      template
    );
    return response.data;
  },

  /**
   * Update a shift template
   */
  async updateShiftTemplate(
    branchId: number,
    templateId: number,
    template: CreateShiftTemplateDto
  ): Promise<Branch> {
    const response = await apiClient.put<Branch>(
      `/branches/${branchId}/shift-templates/${templateId}`,
      template
    );
    return response.data;
  },
};

export default BranchApi;
