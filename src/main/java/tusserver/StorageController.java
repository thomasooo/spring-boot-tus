package tusserver;

import org.springframework.core.env.Environment;
import tusserver.storage.model.TusFile;
import tusserver.exceptions.TusBadRequestException;
import tusserver.exceptions.TusDBFileNotFoundException;
import tusserver.storage.model.TusFilesRepository;
import tusserver.storage.services.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.Objects;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/")
public class StorageController {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private Environment environment;

    @Autowired
    private TusFilesRepository repo;

    @Autowired
    private StorageService storage;

    @RequestMapping(method = RequestMethod.OPTIONS)
    ResponseEntity<?> processOptions(HttpServletResponse response) throws Exception {

        log.debug("OPTIONS");

        response.setHeader("Access-Control-Expose-Headers", "Tus-Resumable, Tus-Version, Tus-Max-Size, Tus-Extension");
        response.setHeader("Tus-Resumable", "1.0.0");
        response.setHeader("Tus-Version", "1.0.0,0.2.2,0.2.1");
        response.setHeader("Tus-Max-Size", environment.getProperty("tusserver.tusmaxsize"));
        response.setHeader("Tus-Extension", "creation,expiration");
        response.setHeader("Access-Control-Allow-Origin","*");
        response.setHeader("Access-Control-Allow-Methods", "GET,PUT,POST,DELETE");
        response.setStatus(204);
        return null;
    }

    @RequestMapping(method = RequestMethod.OPTIONS, value = "/{uuid}")
    ResponseEntity<?> processOptionsUuid(@PathVariable String uuid, HttpServletResponse response) throws Exception {

        log.debug("OPTIONS START");

        TusFile file = repo.findOne(uuid);
        if(file == null){
            throw new TusDBFileNotFoundException(uuid);
        }

        log.debug("file offset: " + file.getOffset());
        log.debug("OPTIONS END");

        response.setHeader("Access-Control-Expose-Headers", "Tus-Resumable, Tus-Version, Tus-Max-Size, Tus-Extension");
        response.setHeader("Tus-Resumable", "1.0.0");
        response.setHeader("Tus-Version", "1.0.0,0.2.2,0.2.1");
        response.setHeader("Tus-Max-Size", environment.getProperty("tusserver.tusmaxsize"));
        response.setHeader("Tus-Extension", "creation,expiration");
        response.setHeader("Access-Control-Allow-Origin","*");
        response.setHeader("Access-Control-Allow-Methods", "GET,PUT,POST,DELETE");
        response.setHeader("Upload-Offset", Integer.toString(file.getOffset()));
        response.setStatus(204);
        return null;
    }

    @RequestMapping(method = RequestMethod.POST)
    ResponseEntity<?> processPost(@RequestHeader("Upload-Length") Integer uploadLength,
                                  UriComponentsBuilder uriComponentsBuilder,
                                  HttpServletResponse response) throws Exception {

        log.debug("POST START");
        log.debug("Final-Length header value: " + Long.toString(uploadLength));

        if(uploadLength < 1){
            throw new TusBadRequestException("Wrong Final-Length Header");
        }

        if(uploadLength > Long.parseLong(environment.getProperty("tusserver.tusmaxsize"))){
            throw new TusBadRequestException("wrong Final-Length Header, max is: " + environment.getProperty("tusserver.tusmaxsize"));
        }

        TusFile file = new TusFile();
        file.setUploadLength(uploadLength);
        file.setOffset(0);
        file.setCompleted(false);
        file = repo.save(file);

        log.debug("POST END");

        response.setHeader("Access-Control-Expose-Headers", "Location, Tus-Resumable");
        response.setHeader("Location", uriComponentsBuilder.path("/" + file.getUuid()).build().toString());
        response.setHeader("Tus-Resumable", "1.0.0");
        response.setStatus(201);
        return null;
    }

    @RequestMapping(method = RequestMethod.HEAD, value = "/{uuid}")
    ResponseEntity<?> processHead(@PathVariable String uuid, HttpServletResponse response) throws Exception {

        log.debug("HEAD START");
        log.debug("uuid value: " + uuid);

        TusFile file = repo.findOne(uuid);
        if(file == null){
            throw new TusDBFileNotFoundException(uuid);
        }

        log.debug("file offset: " + file.getOffset());
        log.debug("HEAD END");

        response.setHeader("Access-Control-Expose-Headers", "Upload-Offset, Upload-Length, Tus-Resumable");
        response.setHeader("Upload-Offset", Integer.toString(file.getOffset()));
        response.setHeader("Upload-Length", Integer.toString(file.getUploadLength()));
        response.setHeader("Tus-Resumable", "1.0.0");
        response.setStatus(200);
        return null;
    }

    @RequestMapping(method = RequestMethod.PATCH, value = "/{uuid}")
    ResponseEntity<?> processPatch(@RequestHeader("Upload-Offset") Integer uploadOffset,
                                   @RequestHeader("Content-Length") Integer contentLength,
                                   @RequestHeader("Content-Type") String contentType,
                                   @PathVariable String uuid,
                                   InputStream inputStream,
                                   HttpServletResponse response) throws Exception {

        log.debug("PATCH START");
        log.debug("uuid value: " + uuid);
        log.debug("Upload-Offset: " + uploadOffset);
        log.debug("Content-Length: " + contentLength);
        log.debug("Content-Type: " + contentType);

        if(uploadOffset == null || uploadOffset < 0){
            throw new TusBadRequestException("Wrong Offset Header");
        }

        if(contentLength == null || contentLength < 0){
            throw new TusBadRequestException("Wrong Content-Length Header");
        }

        if(!contentType.equals("application/offset+octet-stream")){
            throw new TusBadRequestException("Wrong Content-Type Header");
        }

        TusFile file = repo.findOne(uuid);
        if(file == null){
            throw new TusDBFileNotFoundException(uuid);
        }

        log.debug("TusFile Offset: " + file.getOffset());
        log.debug("TusFile FinalLength: " + file.getUploadLength());

        if(!Objects.equals(uploadOffset, file.getOffset())){
            throw new TusBadRequestException("Offsets are not same.");
        }

        if(file.getUploadLength() < file.getOffset()){
            throw new TusBadRequestException("Wrong upload length.");
        }

        //successful
        if(Objects.equals(file.getUploadLength(), file.getOffset())){
            log.debug("Upload-length == Offset");
            log.debug("PATCH END");
            if(!file.isCompleted()){
                file.setCompleted(true);
                repo.save(file);
            }
            response.setStatus(200);
            return null;
        }

        int newOffset = storage.processStream(uuid, inputStream);

        if(newOffset > file.getUploadLength()){
            throw new TusBadRequestException("File is bigger than uploadLength");
        }

        log.debug("New Offset: " + Integer.toString(newOffset));

        file.setOffset(newOffset);
        repo.save(file);

        log.debug("PATCH END");

        response.setHeader("Access-Control-Expose-Headers", "Upload-Offset, Tus-Resumable");
        response.setHeader("Tus-Resumable", "1.0.0");
        response.setHeader("Upload-Offset", Integer.toString(newOffset));
        response.setStatus(204);
        return null;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{uuid}")
    @ResponseBody
    ResponseEntity<Resource> processGet(@PathVariable String uuid) throws Exception {

        log.debug("GET START");
        log.debug("uuid value: " + uuid);

        TusFile file = repo.findOne(uuid);
        if(file == null){
            throw new TusDBFileNotFoundException(uuid);
        }

        if(!file.isCompleted() && Objects.equals(file.getUploadLength(), file.getOffset())){
            log.debug("Upload-length == Offset");
            file.setCompleted(true);
            repo.save(file);
        }

        if(!file.isCompleted()){
            throw new TusBadRequestException("File is not completed.");
        }

        Resource fileResource = storage.loadResource(uuid);

        log.debug("GET END");

        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + fileResource.getFilename() + "\"").body(fileResource);
    }

}
