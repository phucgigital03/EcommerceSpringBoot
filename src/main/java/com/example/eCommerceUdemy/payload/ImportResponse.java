package com.example.eCommerceUdemy.payload;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Data
public class ImportResponse {
    private int totalItems;
    private int successCount;
    private int failureCount;
    private List<ImportResultItem> results;

    public ImportResponse() {
        this.results = new ArrayList<>();
    }

    public void addResult(ImportResultItem result) {
        this.results.add(result);
        if (result.isSuccess()) {
            this.successCount++;
        } else {
            this.failureCount++;
        }
    }

    @Setter
    @Getter
    public static class ImportResultItem {
        private String productName;
        private int rowNumber;
        private boolean success;
        private String errorMessage;

        public ImportResultItem(String productName, int rowNumber, boolean success, String errorMessage) {
            this.productName = productName;
            this.rowNumber = rowNumber;
            this.success = success;
            this.errorMessage = errorMessage;
        }

    }
}
