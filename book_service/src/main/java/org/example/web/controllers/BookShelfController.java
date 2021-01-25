package org.example.web.controllers;

import java.net.MalformedURLException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.Valid;

import org.apache.log4j.Logger;
import org.example.app.services.BookService;
import org.example.app.services.FileRepository;
import org.example.web.dto.Book;
import org.example.web.dto.BookToFilter;
import org.example.web.dto.BookToRemove;
import org.example.web.validation.SaveGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping(value = "/books")
public class BookShelfController {

    private final Logger logger = Logger.getLogger(BookShelfController.class);
    private final BookService bookService;
    private final FileRepository fileRepository;

    @Autowired
    public BookShelfController(BookService bookService, FileRepository fileRepository) {
        this.bookService = bookService;
        this.fileRepository = fileRepository;
    }

    @GetMapping("/shelf")
    public String books(Model model, BookToFilter bookToFilter, BindingResult result) {
        if (result.hasErrors()) {
            logger.warn(result.getAllErrors());

            model.addAttribute("book", new Book());
            model.addAttribute("bookToRemove", new BookToRemove());
            model.addAttribute("bookList", bookService.getAllBooks());
            model.addAttribute("files", fileRepository.getAllFiles());

            return "book_shelf";
        }

        logger.info("got book shelf");
        logger.info("filter books by: " + bookToFilter.getAuthor() + " " + bookToFilter.getTitle()
            + " " + bookToFilter.getSize());

        List<Book> books = bookService.getAllBooks();
        Stream<Book> stream = books.stream();

        if (bookToFilter.getAuthor() != null && !bookToFilter.getAuthor().isEmpty()) {
            stream = stream.filter(book -> book.getAuthor().startsWith(bookToFilter.getAuthor()));
        }
        if (bookToFilter.getTitle() != null && !bookToFilter.getTitle().isEmpty()) {
            stream = stream.filter(book -> book.getTitle().startsWith(bookToFilter.getTitle()));
        }
        if (bookToFilter.getSize() != null) {
            stream = stream.filter(book -> book.getSize().equals(bookToFilter.getSize()));
        }

        model.addAttribute("book", new Book());
        model.addAttribute("bookToRemove", new BookToRemove());
        model.addAttribute("bookToFilter", new BookToFilter());
        model.addAttribute("bookList", stream.collect(Collectors.toList()));
        model.addAttribute("files", fileRepository.getAllFiles());

        return "book_shelf";
    }

    @PostMapping("/save")
    public String saveBook(@Validated(SaveGroup.class) Book book, BindingResult result, Model model) {
        if (result.hasErrors()) {
            logger.warn(result.getAllErrors());

            model.addAttribute("bookToRemove", new BookToRemove());
            model.addAttribute("bookToFilter", new BookToFilter());
            model.addAttribute("bookList", bookService.getAllBooks());
            model.addAttribute("files", fileRepository.getAllFiles());

            return "book_shelf";
        }

        bookService.saveBook(book);
        logger.info("current repository size: " + bookService.getAllBooks().size());

        return "redirect:/books/shelf";
    }

    @PostMapping("/remove")
    public String removeBook(@Valid BookToRemove bookToRemove, BindingResult result, Model model) {
        if (result.hasErrors()) {
            logger.warn(result.getAllErrors());

            model.addAttribute("book", new Book());
            model.addAttribute("bookToFilter", new BookToFilter());
            model.addAttribute("bookList", bookService.getAllBooks());
            model.addAttribute("files", fileRepository.getAllFiles());

            return "book_shelf";
        }

        if (bookToRemove.getId() == null && bookToRemove.getAuthor().isEmpty() &&
            bookToRemove.getTitle().isEmpty() && bookToRemove.getSize() == null) {
            logger.warn("empty book");
        } else if (!bookService.remove(bookToRemove)) {
            logger.warn("book: " + bookToRemove + " not found:");
        }

        return "redirect:/books/shelf";
    }

    @PostMapping("/uploadFile")
    public String uploadFile(@RequestParam("file") MultipartFile file) throws Exception {

        if (file.getOriginalFilename().isEmpty()) {
            logger.warn("file is empty");
            return "redirect:/books/shelf";
        }

        fileRepository.uploadFile(file);

        return "redirect:/books/shelf";
    }

    @GetMapping(value = "/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam(value = "name") String name)
        throws MalformedURLException {

        Resource file = fileRepository.loadFile(name);

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
            .body(file);
    }
}