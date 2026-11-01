package com.finnexa.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finnexa.dto.ReportResponse;
import com.finnexa.entity.Sale;
import com.finnexa.exception.BadRequestException;
import com.finnexa.repository.SaleRepository;

@Service
public class ReportService {

	private final SaleRepository saleRepository;

	public ReportService(SaleRepository saleRepository) {
		this.saleRepository = saleRepository;
	}

	@Transactional(readOnly = true)
	public ReportResponse getFinancialSummary(LocalDateTime startDate, LocalDateTime endDate) {

		if (startDate == null || endDate == null) {
			throw new BadRequestException("Start date and end date are required.");
		}

		if (startDate.isAfter(endDate)) {
			throw new BadRequestException("Start date must not be after end date.");
		}

		List<Sale> completedSales = saleRepository.findByStatusAndSaleDateBetween(Sale.SaleStatus.COMPLETED, startDate,
				endDate);

		long cancelledSales = saleRepository
				.findByStatusAndSaleDateBetween(Sale.SaleStatus.CANCELLED, startDate, endDate).size();

		BigDecimal totalRevenue = completedSales.stream().map(Sale::getTotalAmount).filter(amount -> amount != null)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		long totalSales = completedSales.size();

		BigDecimal averageSaleValue = totalSales == 0 ? BigDecimal.ZERO
				: totalRevenue.divide(BigDecimal.valueOf(totalSales), 2, RoundingMode.HALF_UP);

		return new ReportResponse(totalRevenue, totalSales, averageSaleValue, cancelledSales);
	}
}