package com.example.eCommerceUdemy.service;

import com.example.eCommerceUdemy.model.Product;
import com.example.eCommerceUdemy.payload.ImportResponse;
import com.example.eCommerceUdemy.repository.CategoryRepository;
import com.example.eCommerceUdemy.repository.ProductRepository;
import com.example.eCommerceUdemy.util.ExcelHelper;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Service
public class ProductImportServiceImpl implements ProductImportService {
    @Autowired
    CategoryRepository categoryRepository;
    @Autowired
    ProductRepository productRepository;

    @Override
    public ImportResponse importProductsFromExcel(MultipartFile file) {
        try {
            Workbook workbook = WorkbookFactory.create(file.getInputStream());
            Sheet sheet = workbook.getSheet(ExcelHelper.SHEET);
            Iterator<Row> rows = sheet.iterator();

            ImportResponse response = new ImportResponse();
            // List<Product> successfulProducts = new ArrayList<>();

            // Skip header row
            Map<String, Integer> columnIndexMap = new HashMap<>();
            if (rows.hasNext()) {
                Row headerRow = rows.next();
                for(Cell cell : headerRow){
                    columnIndexMap.put(cell.getStringCellValue().trim(), cell.getColumnIndex());
                }
            }
//            columnIndexMap.forEach((k,v)->{
//                System.out.println("key: " +  k);
//                System.out.println("value: "+ columnIndexMap.get(k));
//            });

            int rowNumber = 1; // Start from 1 because row 0 is header
            while (rows.hasNext()) {
                rowNumber++;
                Row currentRow = rows.next();
                try {
                    // Extract product from the current row
                    Product product = ExcelHelper.extractProductFromRow(currentRow, columnIndexMap, categoryRepository);

                    System.out.println("product from excel: " + product);
                    // Save product to database
                    productRepository.save(product);
                    // successfulProducts.add(product);

                    // Record successful import
                    response.addResult(new ImportResponse.ImportResultItem(
                            product.getProductName(),
                            rowNumber,
                            true,
                            null
                    ));
                } catch (Exception e) {
                    // Get product name for error reporting
                    String productName = "Unknown";
                    try {
                        Cell nameCell = currentRow.getCell(0); // Product name is at index 0
                        if (nameCell != null) {
                            productName = nameCell.getStringCellValue();
                        }
                    } catch (Exception ex) {
                        productName = "Row " + rowNumber;
                    }

                    // Record failed import
                    response.addResult(new ImportResponse.ImportResultItem(
                            productName,
                            rowNumber,
                            false,
                            e.getMessage()
                    ));
                }
            }

            workbook.close();
            response.setTotalItems(rowNumber - 1); // Exclude header row
            return response;
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse Excel file: " + e.getMessage());
        }
    }
}
