package com.example.eCommerceUdemy.util;

import com.example.eCommerceUdemy.model.Category;
import com.example.eCommerceUdemy.model.Product;
import com.example.eCommerceUdemy.repository.CategoryRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ExcelHelper {
    public static String TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    public static String[] HEADERS = {"Product ID", "Product Name", "Description", "Price", "Special Price",
            "Discount", "Quantity", "Image", "Category Name"};
    public static String SHEET = "productDBs";

    public static boolean hasExcelFormat(MultipartFile file) {
        return TYPE.equals(file.getContentType());
    }

    /*
     * Extracts a single product from an Excel row.
     * Returns null if the row cannot be parsed.
     */
    public static Product extractProductFromRow(Row row, Map<String, Integer> columnIndexMap, CategoryRepository categoryRepository) throws Exception {
        int rowIndex = row.getRowNum(); // Excel rows start from 0
        System.out.println("Current row index from excel file: " + rowIndex);
        try {
            Product product = new Product();

            try {
                product.setProductName(getStringCellValue(row.getCell(columnIndexMap.get("Product Name"))));
            } catch (Exception e) {
                throw new Exception("Row " + rowIndex + " - Error at column 'Product Name': " + e.getMessage());
            }

            try {
                product.setDescription(getStringCellValue(row.getCell(columnIndexMap.get("Description"))));
            } catch (Exception e) {
                throw new Exception("Row " + rowIndex + " - Error at column 'Description': " + e.getMessage());
            }

            try {
                product.setImage("default.jpg"); // Or getStringCellValue(...) if reading image name
            } catch (Exception e) {
                throw new Exception("Row " + rowIndex + " - Error at column 'Image': " + e.getMessage());
            }

            try {
                product.setPrice(getNumericCellValue(row.getCell(columnIndexMap.get("Price"))));
            } catch (Exception e) {
                throw new Exception("Row " + rowIndex + " - Error at column 'Price': " + e.getMessage());
            }

            try {
                product.setDiscount(getNumericCellValue(row.getCell(columnIndexMap.get("Discount"))));
                double specialPrice = product.getPrice() - (product.getPrice() * product.getDiscount() * 0.01);
                product.setSpecialPrice(specialPrice);
            } catch (Exception e) {
                throw new Exception("Row " + rowIndex + " - Error at column 'Discount': " + e.getMessage());
            }

            try {
                product.setQuantity((int) getNumericCellValue(row.getCell(columnIndexMap.get("Quantity"))));
            } catch (Exception e) {
                throw new Exception("Row " + rowIndex + " - Error at column 'Quantity': " + e.getMessage());
            }

            try {
                String categoryName = getStringCellValue(row.getCell(columnIndexMap.get("Category Name")));
                Category categoryDB = categoryRepository.findByCategoryName(categoryName);
                if (categoryDB == null) {
                    throw new Exception("Category '" + categoryName + "' not found");
                }
                product.setCategory(categoryDB);
            } catch (Exception e) {
                throw new Exception("Row " + rowIndex + " - Error at column 'Category': " + e.getMessage());
            }

            // Can validate product here
            return product;
        } catch (Exception e) {
            // Propagate enriched error message
            throw new Exception(e.getMessage());
        }
    }

    // Helper methods to handle different cell types
    private static String getStringCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    private static double getNumericCellValue(Cell cell) throws Exception {
        if (cell == null) {
            throw new Exception("Cell is empty");
        }
        switch (cell.getCellType()) {
            case NUMERIC:
                return cell.getNumericCellValue();
            case STRING:
                try {
                    return Double.parseDouble(cell.getStringCellValue());
                } catch (NumberFormatException e) {
                    throw new Exception("Invalid number format: " + cell.getStringCellValue());
                }
            default:
                throw new Exception("Cell value is not a number");
        }
    }

    public static ByteArrayInputStream productsToExcel(List<Product> products) {
        try (
                Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()
        ) {

            Sheet sheet = workbook.createSheet(SHEET);

            // Header
            Row headerRow = sheet.createRow(0);

            for (int col = 0; col < HEADERS.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(HEADERS[col]);
            }

            int rowIdx = 1;
            for (Product product : products) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(product.getProductId());
                row.createCell(1).setCellValue(product.getProductName());
                row.createCell(2).setCellValue(product.getDescription());
                row.createCell(3).setCellValue(product.getPrice());
                row.createCell(4).setCellValue(product.getSpecialPrice());
                row.createCell(5).setCellValue(product.getDiscount());
                row.createCell(6).setCellValue(product.getQuantity());
                row.createCell(7).setCellValue(product.getImage());
                row.createCell(8).setCellValue(product.getCategory() != null ?
                        product.getCategory().getCategoryName() : "nothing");
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("fail to export data to Excel file: " + e.getMessage());
        }
    }

}
