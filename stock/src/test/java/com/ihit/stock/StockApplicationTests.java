package com.ihit.stock;

import com.ihit.stock.model.AppUser;
import com.ihit.stock.model.StockMarketData;
import com.ihit.stock.repository.AppUserRepository;
import com.ihit.stock.repository.StockMarketDataRepository;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(properties = {
		"spring.datasource.url=jdbc:h2:mem:stock-test;DB_CLOSE_DELAY=-1",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
class StockApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AppUserRepository userRepository;

	@Autowired
	private StockMarketDataRepository stockMarketDataRepository;

	@Test
	void contextLoads() {
	}

	@Test
	void homeRedirectsToDashboard() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/dashboard"));
	}

	@Test
	void adminCanLoginAndSeeUserList() throws Exception {
		mockMvc.perform(post("/login")
						.param("username", "admin")
						.param("password", "admin123")
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/dashboard"))
				.andExpect(authenticated().withUsername("admin"));

		mockMvc.perform(get("/admin/users").with(httpBasic("admin", "admin123")))
				.andExpect(status().isOk())
				.andExpect(view().name("users"))
				.andExpect(model().attributeExists("users"));
	}

	@Test
	void registrationCreatesPendingUserThatNeedsAdminApproval() throws Exception {
		mockMvc.perform(post("/register")
						.param("username", "newuser")
						.param("password", "password123")
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));

		mockMvc.perform(get("/dashboard").with(httpBasic("newuser", "password123")))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(get("/admin/users").with(httpBasic("admin", "admin123")))
				.andExpect(status().isOk())
				.andExpect(model().attributeExists("users"));

		AppUser newUser = userRepository.findByUsernameIgnoreCase("newuser").orElseThrow();
		mockMvc.perform(post("/admin/users/{id}/approve", newUser.getId())
						.with(httpBasic("admin", "admin123"))
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/users"));

		mockMvc.perform(get("/dashboard").with(httpBasic("newuser", "password123")))
				.andExpect(status().isOk())
				.andExpect(view().name("dashboard"));
	}

	@Test
	void nonAdminCannotOpenUserList() throws Exception {
		mockMvc.perform(post("/register")
						.param("username", "regular")
						.param("password", "password123")
						.with(csrf()))
				.andExpect(status().is3xxRedirection());

		AppUser regular = userRepository.findByUsernameIgnoreCase("regular").orElseThrow();
		mockMvc.perform(post("/admin/users/{id}/approve", regular.getId())
						.with(httpBasic("admin", "admin123"))
						.with(csrf()))
				.andExpect(status().is3xxRedirection());

		mockMvc.perform(get("/admin/users").with(httpBasic("regular", "password123")))
				.andExpect(status().isForbidden());
	}

	@Test
	void approvedUserCanUpdateProfileAndChangePassword() throws Exception {
		mockMvc.perform(post("/register")
						.param("username", "profileuser")
						.param("password", "password123")
						.with(csrf()))
				.andExpect(status().is3xxRedirection());

		AppUser profileUser = userRepository.findByUsernameIgnoreCase("profileuser").orElseThrow();
		mockMvc.perform(post("/admin/users/{id}/approve", profileUser.getId())
						.with(httpBasic("admin", "admin123"))
						.with(csrf()))
				.andExpect(status().is3xxRedirection());

		mockMvc.perform(post("/user/profile")
						.with(httpBasic("profileuser", "password123"))
						.with(csrf())
						.param("fullName", "Profile User")
						.param("email", "profile@example.com")
						.param("phoneNumber", "01700000000")
						.param("gender", "Other")
						.param("address", "Dhaka")
						.param("profilePictureUrl", "https://example.com/profile.png")
						.param("websiteSocialLinks", "https://example.com"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/user/profile"));

		AppUser updatedUser = userRepository.findByUsernameIgnoreCase("profileuser").orElseThrow();
		org.assertj.core.api.Assertions.assertThat(updatedUser.getFullName()).isEqualTo("Profile User");
		org.assertj.core.api.Assertions.assertThat(updatedUser.getEmail()).isEqualTo("profile@example.com");

		mockMvc.perform(post("/user/password")
						.with(httpBasic("profileuser", "password123"))
						.with(csrf())
						.param("oldPassword", "password123")
				.param("newPassword", "newpass123")
				.param("confirmPassword", "newpass123"))
		.andExpect(status().is3xxRedirection())
		.andExpect(redirectedUrl("/login?passwordChanged"));

		mockMvc.perform(get("/dashboard").with(httpBasic("profileuser", "newpass123")))
				.andExpect(status().isOk())
				.andExpect(view().name("dashboard"));
	}

	@Test
	void emailAddressMustBeUnique() throws Exception {
		mockMvc.perform(post("/register")
						.param("username", "emailone")
						.param("password", "password123")
						.with(csrf()))
				.andExpect(status().is3xxRedirection());
		mockMvc.perform(post("/register")
						.param("username", "emailtwo")
						.param("password", "password123")
						.with(csrf()))
				.andExpect(status().is3xxRedirection());

		AppUser emailOne = userRepository.findByUsernameIgnoreCase("emailone").orElseThrow();
		AppUser emailTwo = userRepository.findByUsernameIgnoreCase("emailtwo").orElseThrow();
		mockMvc.perform(post("/admin/users/{id}/approve", emailOne.getId())
						.with(httpBasic("admin", "admin123"))
						.with(csrf()))
				.andExpect(status().is3xxRedirection());
		mockMvc.perform(post("/admin/users/{id}/approve", emailTwo.getId())
						.with(httpBasic("admin", "admin123"))
						.with(csrf()))
				.andExpect(status().is3xxRedirection());

		mockMvc.perform(post("/user/profile")
						.with(httpBasic("emailone", "password123"))
						.with(csrf())
						.param("email", "same@example.com"))
				.andExpect(status().is3xxRedirection());

		mockMvc.perform(post("/user/profile")
						.with(httpBasic("emailtwo", "password123"))
						.with(csrf())
						.param("email", "same@example.com"))
				.andExpect(status().is3xxRedirection());

		AppUser unchangedUser = userRepository.findByUsernameIgnoreCase("emailtwo").orElseThrow();
		org.assertj.core.api.Assertions.assertThat(unchangedUser.getEmail()).isNull();
	}

	@Test
	void stockExcelUploadCanPreviewSaveReplaceEditAndDeleteByTradingCode() throws Exception {
		MvcResult previewResult = mockMvc.perform(multipart("/stocks/upload")
						.file(stockWorkbook("TST", "10.50"))
						.with(httpBasic("admin", "admin123"))
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/stocks/upload"))
				.andReturn();

		MockHttpSession session = (MockHttpSession) previewResult.getRequest().getSession(false);
		mockMvc.perform(post("/stocks/save")
						.session(session)
						.with(httpBasic("admin", "admin123"))
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/stocks/data"));

		List<StockMarketData> savedRows = stockMarketDataRepository.findAll();
		org.assertj.core.api.Assertions.assertThat(savedRows)
				.filteredOn(row -> row.getTradingCode().equals("TST"))
				.hasSize(1);

		MvcResult replacePreviewResult = mockMvc.perform(multipart("/stocks/upload")
						.file(stockWorkbook("TST", "12.75"))
						.with(httpBasic("admin", "admin123"))
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andReturn();

		MockHttpSession replaceSession = (MockHttpSession) replacePreviewResult.getRequest().getSession(false);
		mockMvc.perform(post("/stocks/save")
						.session(replaceSession)
						.with(httpBasic("admin", "admin123"))
						.with(csrf()))
				.andExpect(status().is3xxRedirection());

		List<StockMarketData> replacedRows = stockMarketDataRepository.findAll().stream()
				.filter(row -> row.getTradingCode().equals("TST"))
				.toList();
		org.assertj.core.api.Assertions.assertThat(replacedRows).hasSize(1);
		org.assertj.core.api.Assertions.assertThat(replacedRows.get(0).getClosep()).isEqualByComparingTo("12.75");

		StockMarketData record = replacedRows.get(0);
		mockMvc.perform(post("/stocks/{id}/edit", record.getId())
						.with(httpBasic("admin", "admin123"))
						.with(csrf())
						.param("date", "2026-01-01")
						.param("tradingCode", "TST")
						.param("ltp", "14.00")
						.param("high", "15.00")
						.param("low", "13.00")
						.param("openp", "13.50")
						.param("closep", "14.25")
						.param("ycp", "12.00")
						.param("tradeValue", "1.75")
						.param("volume", "5000"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/stocks/data"));

		StockMarketData edited = stockMarketDataRepository.findById(record.getId()).orElseThrow();
		org.assertj.core.api.Assertions.assertThat(edited.getLtp()).isEqualByComparingTo(new BigDecimal("14.00"));

		mockMvc.perform(post("/stocks/delete-by-code")
						.with(httpBasic("admin", "admin123"))
						.with(csrf())
						.param("tradingCode", "TST"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/stocks/data"));

		org.assertj.core.api.Assertions.assertThat(stockMarketDataRepository.findAll().stream()
				.noneMatch(row -> row.getTradingCode().equals("TST"))).isTrue();
	}

	private MockMultipartFile stockWorkbook(String tradingCode, String closep) throws Exception {
		try (XSSFWorkbook workbook = new XSSFWorkbook();
			 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			Sheet sheet = workbook.createSheet("data");
			Row header = sheet.createRow(0);
			String[] columns = {"DATE", "TRADING CODE", "LTP", "HIGH", "LOW", "OPENP", "CLOSEP", "YCP", "TRADE VALUE", "VOLUME"};
			for (int i = 0; i < columns.length; i++) {
				header.createCell(i).setCellValue(columns[i]);
			}

			Row row = sheet.createRow(1);
			row.createCell(0).setCellValue("2026-01-01");
			row.createCell(1).setCellValue(tradingCode);
			row.createCell(2).setCellValue("10.00");
			row.createCell(3).setCellValue("11.00");
			row.createCell(4).setCellValue("9.00");
			row.createCell(5).setCellValue("9.50");
			row.createCell(6).setCellValue(closep);
			row.createCell(7).setCellValue("8.00");
			row.createCell(8).setCellValue("1.25");
			row.createCell(9).setCellValue("1000");

			workbook.write(outputStream);
			return new MockMultipartFile(
					"file",
					"stock.xlsx",
					"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
					outputStream.toByteArray());
		}
	}
}
