package tusserver.storage.services;

import org.springframework.core.io.Resource;

import java.io.InputStream;

public interface StorageService {
    int processStream(String uuid, InputStream inputStream) throws Exception;
    Resource loadResource(String uuid) throws Exception;
}
