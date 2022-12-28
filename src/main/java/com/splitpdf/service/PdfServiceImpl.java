package com.splitpdf.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.azure.ai.formrecognizer.DocumentAnalysisClient;
import com.azure.ai.formrecognizer.DocumentAnalysisClientBuilder;
import com.azure.ai.formrecognizer.models.AnalyzeResult;
import com.azure.ai.formrecognizer.models.DocumentOperationResult;
import com.azure.ai.formrecognizer.models.DocumentTable;
import com.azure.ai.formrecognizer.models.DocumentTableCell;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.polling.SyncPoller;
import com.splitpdf.model.PdfModel;

import io.micronaut.http.multipart.CompletedFileUpload;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class PdfServiceImpl implements PdfService {
	@Override
	public PdfModel pdfsplit(CompletedFileUpload file, PdfModel model)
			throws IOException, ClassNotFoundException, SQLException {
		XSSFRow row;
		String name = file.getFilename();
		int sp = model.getStartPage();
		int ep = model.getEndPage();
		byte[] filedata = file.getBytes();

		PDDocument document = PDDocument.load(filedata);
		String home = System.getProperty("user.home");

		File splitfile = new File(home + "/Downloads/");

		Splitter splitter = new Splitter();
		splitter.setStartPage(sp);
		splitter.setEndPage(ep);
		List<PDDocument> splitpage = splitter.split(document);

		PDDocument newSplitDoc = new PDDocument();
		for (PDDocument mydoc : splitpage) {
			System.out.println("in for loop");
			newSplitDoc.addPage(mydoc.getPage(0));
		}
		newSplitDoc.save(splitfile + "\\out.pdf");
//			newSplitDoc.close();
		System.out.println("file split success");
		String extPattern = "(?<!^)[.]" + (".*");
//			
		PDStream st = new PDStream(newSplitDoc);
		byte[] d = st.toByteArray();

		PDPageTree page = newSplitDoc.getPages();
//			for(int p=0 ; p<page.getCount(); p++) {
		PDPage g = page.get(0);

		InputStream stream = g.getContents();

		byte[] bb = stream.readAllBytes();
//			
//			FileOutputStream out = new FileOutputStream("input.pdf");
//			out.write(bb);
//			System.out.println(out);
//			out.close();

		Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
		String connectionUrl = "jdbc:sqlserver://localhost;user=Anemoi;password=Anemoi@123";
		Connection conn = DriverManager.getConnection(connectionUrl);

		PreparedStatement statement = conn.prepareStatement("insert into InvestorDB.dbo.filedata values(?)");

		statement.setBytes(1, bb);
		statement.executeUpdate();

		System.out.println("file save in database");

		Statement stt = conn.createStatement();
		ResultSet rs = stt.executeQuery("select * from InvestorDB.dbo.filedata");
		while (rs.next()) {
			byte[] bytedata = rs.getBytes(1);
			System.out.println(bytedata);
//				FileOutputStream out = new FileOutputStream("input.pdf");
//				out.write(bytedata);
//				System.out.println(out);
//				out.close();
			// InputStream inputStream = new ByteArrayInputStream(bytedata);
//				ConvertedDocument convertedDocument = conversionHandler.convert(inputStream, new PdfSaveOptions());
//				convertedDocument.save("converted from byte array.pdf");
//				inputStream.close();
			ByteArrayInputStream bis = new ByteArrayInputStream(bytedata);
//				PDDocument doc = PDDocument.load(bis);
//				doc.save("inpp.pdf");
//				doc.close();

//				PDDocument doc = new PDDocument();
//				PDFStreamEngine engine = new PDFStreamEngine();
//				String home = System.getProperty("user.home");
//				File file2 = new File(home+"/Downloads/" + fileName + ".txt"); 
			// OutputStream out = new FileOutputStream(home+"/Downloads/" + "out.pdf");
			// out.write(bytedata);
//				out.close();

			final String endpoint = "https://secondformrecognizer001.cognitiveservices.azure.com/";
			final String key = "40e2e4174044476596572ce562612d28";
			DocumentAnalysisClient client = new DocumentAnalysisClientBuilder().credential(new AzureKeyCredential(key))
					.endpoint(endpoint).buildClient();

			String modelId = "prebuilt-document";
			String FileName = home + "/Downloads/" + "out.pdf";
			File document1 = new File(FileName);
			XSSFWorkbook workbook = new XSSFWorkbook();

			String replacedFileName = document1.getName().replaceAll(extPattern, "");
			System.out.println(replacedFileName + " replaced name");

			byte[] fileContent = Files.readAllBytes(document1.toPath());

			try (InputStream targetStream = new ByteArrayInputStream(fileContent)) {

				SyncPoller<DocumentOperationResult, AnalyzeResult> analyzeDocumentPoller = client
						.beginAnalyzeDocument(modelId, targetStream, document1.length());
				AnalyzeResult analyzeResult = analyzeDocumentPoller.getFinalResult();
				String content = analyzeResult.getContent().toString();

				List<DocumentTable> tables = analyzeResult.getTables();
				for (int i = 0; i < tables.size(); i++) {

					DocumentTable documentTable = tables.get(i);

					System.out.printf("Table %d has %d rows and %d columns.%n", i, documentTable.getRowCount(),
							documentTable.getColumnCount());

					XSSFSheet spreadsheet = workbook.createSheet("Sheet no" + i);
					XSSFRow newrow = spreadsheet.createRow(0);

					int count = 0;
					int col = 0;

					for (DocumentTableCell dtc : documentTable.getCells()) {
						int rowIndex = dtc.getRowIndex();

						if (count < rowIndex) {
							count++;
							System.out.println(count);
							newrow = spreadsheet.createRow(count);
							col = 0;
						}
						XSSFCell newcell1 = newrow.createCell(col++);
						String keywordContain = dtc.getContent().toString();
						if (keywordContain.equals("EBITDA")) {
							workbook.setSheetName(i, "Income Statement");
						}
						if (keywordContain.equals("Net worth")) {
							workbook.setSheetName(i, "Balance Sheet");
						}
						if (keywordContain.equals("Free Cash Flow")) {
							workbook.setSheetName(i, "Cash Flow");
						}

						newcell1.setCellValue(dtc.getContent().toString());

//					documentTable.getCells().forEach(documentTableCell -> {
//						System.out.printf("Cell '%s', has row index %d and column index %d.%n",
//								documentTableCell.getContent(), documentTableCell.getRowIndex(),
//								documentTableCell.getColumnIndex());
//					
//					});
					}

					System.out.println("=======================Created a new sheet===========================");

				}
				FileOutputStream out1 = new FileOutputStream(new File(home + "/Downloads/" + name + ".xlsx"));
				workbook.write(out1);
				out1.close();

				System.out.printf("Successfully created %d sheets", tables.size());
			}
		}

		return model;
	}

}
