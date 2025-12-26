import { apiClient } from "./ApiClient";

export interface Product {
  id: number;
  sku: string;
  name: string;
  category: string;
  unit: string;
}

export interface Stock {
  id: number;
  branchId: number;
  product: Product;
  quantityOnHand: number;
  reorderThreshold: number;
}

export interface CreateProductDto {
  sku: string;
  name: string;
  category: string;
  unit: string;
}

export interface UpdateStockDto {
  quantityOnHand: number;
  reorderThreshold: number;
}

export interface AddStockDto {
  productId: number;
  quantityOnHand: number;
  reorderThreshold: number;
}

const InventoryApi = {
  /**
   * Get all products
   */
  async getProducts(): Promise<Product[]> {
    const response = await apiClient.get<Product[]>("/inventory/products");
    return response.data;
  },

  /**
   * Create a new product
   */
  async createProduct(product: CreateProductDto): Promise<Product> {
    const response = await apiClient.post<Product>("/inventory/products", product);
    return response.data;
  },

  /**
   * Get stock levels for a branch
   */
  async getBranchStock(branchId: number): Promise<Stock[]> {
    const response = await apiClient.get<Stock[]>(`/inventory/branches/${branchId}/stock`);
    return response.data;
  },

  /**
   * Update stock levels
   */
  async updateStock(stockId: number, updates: UpdateStockDto): Promise<Stock> {
    const response = await apiClient.put<Stock>(`/inventory/stock/${stockId}`, updates);
    return response.data;
  },

  /**
   * Add stock item to a branch
   */
  async addStockItem(branchId: number, stockItem: AddStockDto): Promise<Stock> {
    const response = await apiClient.post<Stock>(`/inventory/branches/${branchId}/stock`, stockItem);
    return response.data;
  },

  /**
   * Get low stock items for a branch
   */
  async getLowStockItems(branchId: number): Promise<Stock[]> {
    const stock = await this.getBranchStock(branchId);
    return stock.filter(s => s.quantityOnHand < s.reorderThreshold);
  },
};

export default InventoryApi;