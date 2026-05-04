import { createClient } from '@supabase/supabase-js';

const SUPABASE_URL = import.meta.env.VITE_SUPABASE_URL || '';
const SUPABASE_SERVICE_ROLE = import.meta.env.VITE_SUPABASE_SERVICE_ROLE || '';
const BUCKET_NAME = import.meta.env.VITE_SUPABASE_BUCKET || 'research-materials';

const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE);

export const supabaseUpload = {
  uploadPdfToSupabase: async (file: File, repositoryId: string): Promise<string> => {
    // Validate file
    if (!file) {
      throw new Error('No file provided');
    }

    if (file.size > 10 * 1024 * 1024) {
      // 10MB max
      throw new Error('File size exceeds 10MB limit');
    }

    if (file.type !== 'application/pdf') {
      throw new Error('Only PDF files are allowed');
    }

    // Generate unique filename
    const timestamp = Date.now();
    const filename = `${repositoryId}/${timestamp}-${file.name}`;

    try {
      // Upload to Supabase Storage
      const { error: uploadError } = await supabase.storage
        .from(BUCKET_NAME)
        .upload(filename, file, {
          upsert: false,
        });

      if (uploadError) {
        throw new Error(`Upload failed: ${uploadError.message}`);
      }

      // Get public URL
      const { data } = supabase.storage.from(BUCKET_NAME).getPublicUrl(filename);
      return data.publicUrl;
    } catch (error) {
      console.error('Supabase upload error:', error);
      throw error;
    }
  },

  deletePdfFromSupabase: async (publicUrl: string): Promise<void> => {
    try {
      // Extract path from public URL
      const url = new URL(publicUrl);
      const path = url.pathname.split(`/storage/v1/object/public/${BUCKET_NAME}/`)[1];

      if (!path) {
        throw new Error('Invalid public URL');
      }

      const { error } = await supabase.storage.from(BUCKET_NAME).remove([path]);

      if (error) {
        throw new Error(`Delete failed: ${error.message}`);
      }
    } catch (error) {
      console.error('Supabase delete error:', error);
      throw error;
    }
  },
};

export default supabaseUpload;
