package noogel.xyz.search.infrastructure.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.rometools.rome.feed.synd.*;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.consts.BaseConsts;
import noogel.xyz.search.infrastructure.dto.OPDSResMetaDataDto;
import noogel.xyz.search.infrastructure.dto.UrlDto;
import org.apache.commons.lang3.StringUtils;

import jakarta.annotation.Nullable;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;

@Slf4j
public class OPDSHelper {

    private static SyndPerson author(String uri) {
        SyndPerson syndPerson = new SyndPersonImpl();
        syndPerson.setName(BaseConsts.AUTHOR_NAME);
        syndPerson.setEmail(BaseConsts.AUTHOR_EMAIL);
        if (StringUtils.isNotBlank(uri)) {
            syndPerson.setUri(uri);
        }
        return syndPerson;
    }

    public static SyndFeed index(String uri, List<SyndLink> links, List<SyndEntry> entries) {
        SyndFeed feed = new SyndFeedImpl();
        feed.setFeedType(BaseConsts.FEED_TYPE);
        feed.setAuthors(Collections.singletonList(author(uri)));
        feed.setTitle(BaseConsts.NAME);
        feed.setDescription(BaseConsts.DESC);
        feed.setLinks(links);
        feed.setEntries(entries);
        return feed;
    }

    public static List<SyndLink> ofRootSyndLink(UrlDto url) {
        SyndLink startLink = new SyndLinkImpl();
        startLink.setRel("start");
        startLink.setType(BaseConsts.APPLICATION_ATOM_XML_FEED_CATALOG);
        startLink.setHref(url.getRequestUrl());

//        SyndLink selfLink = new SyndLinkImpl();
//        selfLink.setRel("self");
//        selfLink.setType(BaseConsts.APPLICATION_ATOM_XML_FEED_CATALOG);
//        selfLink.setHref(url.getRequestUrl());

        SyndLink searchLink = new SyndLinkImpl();
        searchLink.setRel("search");
        searchLink.setTitle("Search");
        searchLink.setType(BaseConsts.APPLICATION_ATOM_XML);
        searchLink.setHref(url.getSearchUrl());

        ArrayList<SyndLink> objects = new ArrayList<>();
        objects.add(startLink);
//        objects.add(selfLink);
        objects.add(searchLink);
        return objects;
    }

    public static List<SyndLink> ofPageSyndLink(UrlDto url, int nextOffset, int total) {

        SyndLink firstLink = new SyndLinkImpl();
        firstLink.setRel("first");
        firstLink.setType(BaseConsts.APPLICATION_ATOM_XML_FEED_CATALOG);
        Map<String, String> firstParams = url.getParameters();
        firstParams.put("offset", "0");
        firstLink.setHref(UrlDto.buildRequestUrl(url.getRequestUrl(), firstParams));

        SyndLink lastLink = new SyndLinkImpl();
        lastLink.setRel("last");
        lastLink.setType(BaseConsts.APPLICATION_ATOM_XML_FEED_CATALOG);
        Map<String, String> lastParams = url.getParameters();
        lastParams.put("offset", Math.max(total - BaseConsts.DEFAULT_LIMIT, 0) + "");
        lastLink.setHref(UrlDto.buildRequestUrl(url.getRequestUrl(), lastParams));

        SyndLink nextLink = new SyndLinkImpl();
        nextLink.setRel("next");
        nextLink.setType(BaseConsts.APPLICATION_ATOM_XML_FEED_CATALOG);
        Map<String, String> nextParams = url.getParameters();
        nextParams.put("offset", Math.min(total, nextOffset) + "");
        nextLink.setHref(UrlDto.buildRequestUrl(url.getRequestUrl(), nextParams));

        ArrayList<SyndLink> objects = new ArrayList<>();
        objects.add(firstLink);
        objects.add(lastLink);
        objects.add(nextLink);
        return objects;
    }

    public static SyndEntry ofSyndEntry(String title, String href, String content) {
        SyndLink syndLink = new SyndLinkImpl();
        syndLink.setRel("subsection");
        syndLink.setHref(href);
        syndLink.setType("application/atom+xml;profile=opds-catalog;kind=navigation");

        SyndContent syndContent = new SyndContentImpl();
        syndContent.setType("text");
        syndContent.setValue(content);


        SyndEntry syndEntry = new SyndEntryImpl();
        syndEntry.setTitle(title);
        syndEntry.setContents(Collections.singletonList(syndContent));
        syndEntry.setLinks(Collections.singletonList(syndLink));

        return syndEntry;
    }

    @Nullable
    public static OPDSResMetaDataDto readMetaData(String dir) {
        File metaFile = new File(String.format("%s/metadata.opf", dir));
        if (metaFile.exists()) {
            try {
                Charset charset = FileHelper.detectCharset(metaFile);
                Path metaPath = Paths.get(metaFile.toURI());
                String text = Files.readString(metaPath, charset);
                JsonNode jsonNode = XmlHelper.parseXml(text);

                if (Objects.nonNull(jsonNode)) {
                    OPDSResMetaDataDto dto = new OPDSResMetaDataDto();
                    dto.setAbsoluteDir(dir);
                    boolean version2 = Optional.of(jsonNode).map(t -> t.get("version"))
                            .map(JsonNode::asText).filter(t -> t.contains("2.0")).isPresent();
                    if (version2) {
                        Optional.of(jsonNode.findPath("metadata").findPath("identifier"))
                                .filter(JsonNode::isArray).ifPresent(t -> {
                                    ArrayNode arrayNode = (ArrayNode) t;
                                    for (JsonNode node : arrayNode) {
                                        String id = Optional.of(node.get("scheme")).filter(JsonNode::isTextual)
                                                .map(JsonNode::asText).orElse("");
                                        String val = Optional.of(node.get("")).filter(JsonNode::isTextual)
                                                .map(JsonNode::asText).orElse("");
                                        switch (id) {
                                            case "calibre":
                                                dto.setId(val);
                                                break;
                                            case "uuid":
                                                dto.setUuid(val);
                                                break;
                                            case "ISBN":
                                                dto.setISBN(val);
                                                break;
                                            case "MOBI-ASIN":
                                                dto.setMobiASIN(val);
                                                break;
                                        }
                                    }
                                });
                        Optional.of(jsonNode.findPath("metadata").findPath("title")).filter(JsonNode::isTextual)
                                .map(JsonNode::asText).map(AuthorHelper::format).ifPresent(dto::setTitle);

                        Optional.of(jsonNode).map(t -> t.get("metadata")).map(t -> t.get("creator"))
                                .ifPresent(t -> {
                                    List<String> authors = new ArrayList<>();
                                    Consumer<JsonNode> fn = (ti) -> Optional.of(ti).map(k -> k.get("")).map(JsonNode::toString)
                                            .map(AuthorHelper::format).ifPresent(authors::add);
                                    if (t.isArray()) {
                                        t.forEach(fn);
                                    } else {
                                        fn.accept(t);
                                    }
                                    dto.setCreator(authors);
                                });

                        Optional.of(jsonNode.findPath("metadata").findPath("contributor").findPath(""))
                                .filter(JsonNode::isTextual).map(JsonNode::asText).ifPresent(dto::setContributor);

                        Optional.of(jsonNode.findPath("metadata").findPath("date")).filter(JsonNode::isTextual)
                                .map(JsonNode::asText).ifPresent(dto::setDate);
                        Optional.of(jsonNode.findPath("metadata").findPath("publisher")).filter(JsonNode::isTextual)
                                .map(JsonNode::asText).ifPresent(dto::setPublisher);
                        Optional.of(jsonNode.findPath("metadata").findPath("language")).filter(JsonNode::isTextual)
                                .map(JsonNode::asText).ifPresent(dto::setLanguage);
                        Optional.of(jsonNode.findPath("metadata").findPath("description")).filter(JsonNode::isTextual)
                                .map(JsonNode::asText).ifPresent(dto::setDescription);
                        Optional.of(jsonNode.findPath("metadata").findPath("subject")).filter(JsonNode::isTextual)
                                .map(JsonNode::asText).ifPresent(dto::setSubject);

                        Optional.of(jsonNode.findPath("guide").findPath("reference")).filter(t -> {
                            JsonNode type = t.findPath("type");
                            return !type.isMissingNode() && "cover".equals(type.asText());
                        }).map(t -> t.get("href")).map(JsonNode::asText).ifPresent(dto::setCover);

                        Optional.of(jsonNode.findPath("metadata").findPath("meta")).filter(JsonNode::isArray)
                                .map(t -> (ArrayNode) t).ifPresent(t -> {
                                    dto.setMeta(new HashMap<>());
                                    for (JsonNode node : t) {
                                        dto.getMeta().put(
                                                Optional.of(node.findPath("name")).filter(JsonNode::isTextual).map(JsonNode::asText).orElse(""),
                                                Optional.of(node.findPath("content")).filter(JsonNode::isTextual).map(JsonNode::asText).orElse("")
                                        );
                                    }
                                });
                        dto.setSource(jsonNode);
                        return dto;
                    }
                }
            } catch (Exception ex) {
                log.error("autoFindRelationInfo metaFile {}", metaFile.getAbsoluteFile());
            }
        }
        return null;
    }

    public static void main(String[] args) {
        OPDSResMetaDataDto opdsResMetaDataDto = readMetaData("/home/xyz/DockerSharingData/TestSearch/calibre书库/(Ai Er Lan )Xie Zhi Zhu/Dang Ni Lao Le (23110)");
        System.out.println(opdsResMetaDataDto);
    }
}
