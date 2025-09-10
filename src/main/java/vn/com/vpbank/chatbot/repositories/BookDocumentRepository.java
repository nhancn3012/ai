package vn.com.vpbank.chatbot.repositories;


import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import vn.com.vpbank.chatbot.repositories.document.BookDocument;

import java.util.List;


@Repository
public interface BookDocumentRepository extends MongoRepository<BookDocument, String> {
    List<BookDocument> findByTitleContainingIgnoreCase(String title);

    List<BookDocument> findByAuthorContainingIgnoreCase(String author);

    BookDocument findBySourceUrl(String sourceUrl);
}
