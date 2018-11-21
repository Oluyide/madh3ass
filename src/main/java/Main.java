import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.servlet.MultipartConfigElement;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


import static spark.Spark.*;

/**
 * This Java program uses the Spark web application framework to run a webserver.
 * See http://sparkjava.com/ for details on Spark.
 */
public class Main {
	private static final Logger logger = LoggerFactory.getLogger(Main.class);

	private static final String IMAGE_STORAGE_LOCATION = "images/";
	private static final long MAX_FILE_SIZE = 1000000;
	private static final int FILE_THRESHOLD = 1024;
	private static final String IMAGE_PART = "image";

	public static void main(String... args) throws Exception {

		// Tell Spark to use the Environment variable "PORT" set by Heroku. If no PORT variable is set, default to port 5000.
		int port = System.getenv("PORT") == null ? 5000 : Integer.valueOf(System.getenv("PORT"));
		port(port);


		get("/", (req, res) -> "Hello Mobile Developers");

		// matches "GET /hello/foo" and "GET /hello/bar"
		// request.params(":name") is 'foo' or 'bar'
		get("/hello/:name", (request, response) -> {
			return "Hello: " + request.params(":name");
		});

		// this route sesponds with the body of the request
		post("/simple", (request, response) -> {
			return "Request body: " + request.body();
		});

		// this route uses raw byte output for response
		post("/raw", (request, response) -> {
			OutputStream out = response.raw().getOutputStream();
			out.write("Writing to raw!".getBytes());
			out.close();
			return response.raw();
		});


		//For help with the Spark framework this project is using, see http://sparkjava.com/documentation.html

		//Tasks:
		// 1. Define a route for handling a HTTP POST request

		// 2. Get the image from the request, possibly storing it somewhere before proceeding.

		// 3. Process the image using the JHLabs filtering library (you have to add the dependency)

		// 4. Write the processed image to the HTTP response ( Tip: response.raw() can be helpful)

		post("/upload", (request, response) -> {
			logger.info("Request received, retrieving image");
			MultipartConfigElement multipartConfigElement = new MultipartConfigElement(
					IMAGE_STORAGE_LOCATION, MAX_FILE_SIZE, MAX_FILE_SIZE * 10, FILE_THRESHOLD);
			HttpServletRequest raw = request.raw();
			raw.setAttribute("org.eclipse.jetty.multipartConfig",
					multipartConfigElement);
			Part uploadedFile = raw.getPart(IMAGE_PART);
			if (uploadedFile != null) {
				String fileName = uploadedFile.getSubmittedFileName();
				try {
					InputStream in = uploadedFile.getInputStream();
					BufferedImage sourceImage = ImageIO.read(in);
					BufferedImage destImage = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), sourceImage.getType());
					BufferedImage finalImage = blur(sourceImage, destImage);
					ByteArrayOutputStream os = new ByteArrayOutputStream();
					ImageIO.write(finalImage, "jpg", os);
					OutputStream out = response.raw().getOutputStream();
					out.write(os.toByteArray());
					out.close();
					uploadedFile.delete();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				logger.info("File name = " + fileName);
			}
			return response.raw();
		});
	}

	private static BufferedImage blur(BufferedImage sourceImage, BufferedImage destImage) {
		float[] matrix = new float[400];
		for (int i = 0; i < 400; i++) {
			matrix[i] = 1.0f / 400.0f;
		}
		BufferedImageOp op = new ConvolveOp(new Kernel(20, 20, matrix), ConvolveOp.EDGE_NO_OP, null);
		return op.filter(sourceImage, destImage);
	}
}