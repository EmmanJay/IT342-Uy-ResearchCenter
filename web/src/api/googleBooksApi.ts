import type { GoogleBookMetadata } from '../types';

const GOOGLE_BOOKS_API_KEY = import.meta.env.VITE_GOOGLE_BOOKS_API_KEY || '';
const GOOGLE_BOOKS_API_URL = 'https://www.googleapis.com/books/v1/volumes';

export interface GoogleBookVolume {
  volumeInfo: {
    title?: string;
    authors?: string[];
    publishedDate?: string;
    publisher?: string;
    description?: string;
    imageLinks?: {
      thumbnail?: string;
    };
    industryIdentifiers?: Array<{
      type: string;
      identifier: string;
    }>;
  };
}

export const googleBooksApi = {
  fetchBookMetadata: async (query: string): Promise<GoogleBookMetadata | null> => {
    try {
      const searchParams = new URLSearchParams({
        q: query,
        key: GOOGLE_BOOKS_API_KEY,
        maxResults: '1',
      });

      const response = await fetch(`${GOOGLE_BOOKS_API_URL}?${searchParams}`);
      const data = await response.json();

      if (!data.items || data.items.length === 0) {
        return null;
      }

      const volume: GoogleBookVolume = data.items[0];
      const volumeInfo = volume.volumeInfo;

      // Extract ISBN from industryIdentifiers
      let isbn: string | undefined;
      if (volumeInfo.industryIdentifiers) {
        const isbnId = volumeInfo.industryIdentifiers.find(
          (id) => id.type === 'ISBN_13' || id.type === 'ISBN_10'
        );
        isbn = isbnId?.identifier;
      }

      return {
        isbn,
        title: volumeInfo.title,
        authors: volumeInfo.authors || [],
        imageUrl: volumeInfo.imageLinks?.thumbnail,
        publishedDate: volumeInfo.publishedDate,
        publisher: volumeInfo.publisher,
        description: volumeInfo.description,
      };
    } catch (error) {
      console.error('Error fetching book metadata:', error);
      return null;
    }
  },

  searchBooks: async (query: string): Promise<GoogleBookMetadata[]> => {
    try {
      const searchParams = new URLSearchParams({
        q: query,
        key: GOOGLE_BOOKS_API_KEY,
        maxResults: '10',
      });

      const response = await fetch(`${GOOGLE_BOOKS_API_URL}?${searchParams}`);
      const data = await response.json();

      if (!data.items) {
        return [];
      }

      return data.items.map((item: GoogleBookVolume) => {
        const volumeInfo = item.volumeInfo;
        let isbn: string | undefined;
        if (volumeInfo.industryIdentifiers) {
          const isbnId = volumeInfo.industryIdentifiers.find(
            (id) => id.type === 'ISBN_13' || id.type === 'ISBN_10'
          );
          isbn = isbnId?.identifier;
        }

        return {
          isbn,
          title: volumeInfo.title,
          authors: volumeInfo.authors || [],
          imageUrl: volumeInfo.imageLinks?.thumbnail,
          publishedDate: volumeInfo.publishedDate,
          publisher: volumeInfo.publisher,
          description: volumeInfo.description,
        };
      });
    } catch (error) {
      console.error('Error searching books:', error);
      return [];
    }
  },
};

export default googleBooksApi;
