export interface Facts {
  status?: string;
  date?: string;
  explainedIn?: { title: string; to: string };
  supersedes?: string;
  supersededBy?: string;
  readingTime?: number;
  updatedAt?: string;
}
