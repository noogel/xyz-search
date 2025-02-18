package noogel.xyz.search.infrastructure.repo.impl.qdrant;

import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.dto.dao.FileResContentDto;
import noogel.xyz.search.infrastructure.repo.VectorRepo;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class VectorRepoImpl implements VectorRepo {
    @Override
    public void append(FileResContentDto dto) {

    }
}
