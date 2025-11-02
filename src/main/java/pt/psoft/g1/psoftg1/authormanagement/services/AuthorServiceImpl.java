package pt.psoft.g1.psoftg1.authormanagement.services;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pt.psoft.g1.psoftg1.authormanagement.api.AuthorLendingView;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.shared.id.IdGenerator;
import pt.psoft.g1.psoftg1.shared.model.Photo;
import pt.psoft.g1.psoftg1.shared.repositories.PhotoRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthorServiceImpl implements AuthorService {
    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;
    private final AuthorMapper mapper;
    private final PhotoRepository photoRepository;
    private final IdGenerator idGenerator;

    @Override
    public Iterable<Author> findAll() {
        return authorRepository.findAll();
    }

    @Override
    public Optional<Author> findByAuthorNumber(final String authorId) {
        return authorRepository.findByAuthorNumber(authorId);
    }

    @Override
    @Cacheable(value = "authorSearch", key = "'startsWith:' + #name")
    public List<Author> findByName(String name) {
        return authorRepository.findByName_NameStartsWithIgnoreCase(name);
    }

    @Override
    @CacheEvict(value = {"authorById","authorSearch"}, allEntries = true)
    public Author create(final CreateAuthorRequest resource) {
        /*
         * Since photos can be null (no photo uploaded) that means the URI can be null as well.
         * To avoid the client sending false data, photoURI has to be set to any value / null
         * according to the MultipartFile photo object
         *
         * That means:
         * - photo = null && photoURI = null -> photo is removed
         * - photo = null && photoURI = validString -> ignored
         * - photo = validFile && photoURI = null -> ignored
         * - photo = validFile && photoURI = validString -> photo is set
         * */

        MultipartFile photo = resource.getPhoto();
        String photoURI = resource.getPhotoURI();
        if(photo == null && photoURI != null || photo != null && photoURI == null) {
            resource.setPhoto(null);
            resource.setPhotoURI(null);
        }
        final Author author = mapper.create(resource);
        author.assignId(idGenerator.newId());

        return authorRepository.save(author);
    }

    @Override
    @CacheEvict(value = {"authorById","authorSearch"}, allEntries = true)
    public Author partialUpdate(final String authorId, final UpdateAuthorRequest request, final long desiredVersion) {
        final var author = findByAuthorNumber(authorId)
            .orElseThrow(() -> new NotFoundException("Cannot update an object that does not yet exist"));
        author.applyPatch(desiredVersion, request);
        return authorRepository.save(author);
    }
    @Override
    public List<AuthorLendingView> findTopAuthorByLendings() {
        Pageable pageableRules = PageRequest.of(0,5);
        return authorRepository.findTopAuthorByLendings(pageableRules).getContent();
    }

    @Override
    public List<Book> findBooksByAuthorNumber(String authorId){
        return bookRepository.findBooksByAuthorNumber(authorId);
    }

    @Override
    public List<Author> findCoAuthorsByAuthorNumber(String authorId) {
        return authorRepository.findCoAuthorsByAuthorNumber(authorId);
    }

    @Transactional
    @Override
    @CacheEvict(value = {"authorById","authorSearch"}, allEntries = true)
    public Optional<Author> removeAuthorPhoto(String authorId, long desiredVersion) {
        Author author = authorRepository.findByAuthorNumber(authorId)
                .orElseThrow(() -> new NotFoundException("Cannot find reader"));

        if (author.getPhoto() == null) {
            return Optional.of(author);
        }

        Photo photo = author.getPhoto();
        String photoFile = photo.getPhotoFile();

        author.removePhoto(desiredVersion);
        authorRepository.save(author);

        flushIfJpa(authorRepository);

        photoRepository.deleteByPhotoFile(photoFile);
        return Optional.of(author);
    }

    private static void flushIfJpa(Object repo) {
        if (repo instanceof JpaRepository jpaRepo) {
            jpaRepo.flush();
        }
    }
}

