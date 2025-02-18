package noogel.xyz.search.infrastructure.repo;

import noogel.xyz.search.infrastructure.dto.dao.FileResContentDto;

public interface VectorRepo {
    void append(FileResContentDto dto);
}
