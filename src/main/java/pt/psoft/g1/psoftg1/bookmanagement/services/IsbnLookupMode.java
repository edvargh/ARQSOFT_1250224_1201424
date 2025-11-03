package pt.psoft.g1.psoftg1.bookmanagement.services;

public enum IsbnLookupMode {
  ANY,   // return any valid hit from any provider
  BOTH,   // only return ISBNs that appear in at least two providers
  GOOGLE_ONLY, // only use Google Books as provider
  OPENLIBRARY_ONLY // only use Open Library as provider
}
