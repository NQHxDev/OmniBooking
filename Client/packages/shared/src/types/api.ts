export interface ApiResponse<T> {
   message: string;
   errorCode?: string;
   data: T;
   requestId?: string;
   timestamp: string;
}
