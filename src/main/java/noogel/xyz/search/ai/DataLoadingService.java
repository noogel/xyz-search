package noogel.xyz.search.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.JsonReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;

@Service
public class DataLoadingService {

	private static final Logger logger = LoggerFactory.getLogger(DataLoadingService.class);

	@Value("classpath:/ai/data/CVS-Health-2023-Annual-Report.pdf")
	private Resource pdfResource;

	@Value("classpath:/ai/data/bikes.json")
	Resource bikesResouce;

	private final VectorStore vectorStore;

	@Autowired
	public DataLoadingService(VectorStore vectorStore) {
		Assert.notNull(vectorStore, "VectorStore must not be null.");
		this.vectorStore = vectorStore;
	}

	public void load() {
		PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(this.pdfResource,
				PdfDocumentReaderConfig.builder()
					.withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
						.withNumberOfBottomTextLinesToDelete(3)
						.withNumberOfTopPagesToSkipBeforeDelete(1)
						.build())
					.withPagesPerDocument(1)
					.build());

		var tokenTextSplitter = new TokenTextSplitter();

		logger.info(
				"Parsing document, splitting, creating embeddings and storing in vector store...  this will take a while.");
		List<Document> apply = tokenTextSplitter.apply(pdfReader.get());
		this.vectorStore.accept(apply);
		logger.info("Done parsing document, splitting, creating embeddings and storing in vector store");

	}
	public void loadJson() {
		// read json file
		JsonReader jsonReader = new JsonReader(bikesResouce, new ProductMetadataGenerator(),
				"name","shortDescription", "description", "price","tags");

		// create document object
		List<Document> documents = jsonReader.get();

		// add to vectorstore
		vectorStore.add(documents);
	}



}
