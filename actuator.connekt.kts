val host = "http://localhost:8080"

GET("$host/actuator/health") {}

//GET("/path") {
//    header("Accept", "application/json")
//    queryParam("param", "value")
//}
//
//POST("/path") {
//    header("Content-Type", "application/json")
//    body(
//        """
//        {
//            "key": "value"
//        }
//        """.trimIndent()
//    )
//}