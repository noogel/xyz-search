package noogel.xyz.search.infrastructure.qdrant;

public class QdrantUtils {
//
//    public static void main(String[] args) {
//        QdrantClient client = new QdrantClient.Builder()
//                .setStoragePath("/tmp/local_qdrant")
//                .setAutoCreatePath(true)
//                .build();
//
//        // 初始化本地客户端
//        QdrantClient client = new QdrantClient.Builder()
//                .setStoragePath("qdrant_data")
//                .build();
//
//        // 创建集合
//        client.createCollection("products",
//                new VectorParams().setSize(512).setDistance(Distance.COSINE));
//
//        // 插入向量数据
//        List<PointStruct> points = new ArrayList<>();
//        points.add(new PointStruct()
//                .setId(1)
//                .setVector(new float[]{0.1f, 0.2f, ...})
//            .setPayload(Map.of("category", "electronics")));
//        client.upsert("products", points);
//
//        // 执行混合查询
//        SearchResult result = client.search("products")
//                .setVector(new float[]{0.3f, 0.5f, ...})
//            .setFilter(new Filter()
//                .addMust(new Match("category", "electronics")))
//                .execute();
//    }
}
