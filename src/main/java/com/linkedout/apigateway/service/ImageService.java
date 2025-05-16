package com.linkedout.apigateway.service;

import com.linkedout.common.model.dto.ImageUrlDTO;
import com.linkedout.common.model.type.ImageType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;


@Slf4j
@Service
public class ImageService {

	private final Path baseStorageLocation;
	private final String baseUrl;

	// 허용할 이미지 확장자
	private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
		"png", "jpg", "jpeg", "gif", "webp"
	);

	private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

	@Autowired
	public ImageService(
		@Value("${file.upload-dir}") String uploadDir,
		@Value("${file.base-url}") String baseUrl) {
		this.baseStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
		this.baseUrl = baseUrl;

		try {
			Files.createDirectories(this.baseStorageLocation);
		} catch (Exception ex) {
			throw new RuntimeException("이미지 저장 디렉토리를 생성할 수 없습니다.", ex);
		}
	}

	/**
	 * 이미지를 저장하고 URL을 반환합니다.
	 *
	 * @param filePart  업로드된 파일
	 * @param imageType 이미지 타입
	 * @return 저장된 이미지의 URL
	 */
	public Mono<ImageUrlDTO> storeImage(FilePart filePart, ImageType imageType) {
		// 1. 파일 유효성 검사
		return validateFile(filePart)
			.flatMap(valid -> {
				// 2. 파일명 생성
				String originalFilename = filePart.filename();
				String extension = getFileExtension(originalFilename).toLowerCase();
				String newFilename = generateUniqueFilename(extension);

				// 3. 저장 경로 및 URL 생성 - 블로킹 작업을 별도 스케줄러로 이동
				return Mono.fromCallable(() -> {
						// 블로킹 I/O 작업을 여기에 모음
						Path typedPath = baseStorageLocation.resolve(imageType.getPath());
						try {
							Files.createDirectories(typedPath);
						} catch (Exception ex) {
							log.error("이미지 저장 경로 생성 실패", ex);
							throw new RuntimeException("이미지 저장 경로를 생성할 수 없습니다.", ex);
						}

						Path targetLocation = typedPath.resolve(newFilename);
						String imageUrl = baseUrl + imageType.getPath() + "/" + newFilename;

						return new AbstractMap.SimpleEntry<>(targetLocation, imageUrl);
					})
					.subscribeOn(Schedulers.boundedElastic())  // 블로킹 I/O 작업을 위한 전용 스케줄러
					.flatMap(entry -> {
						Path targetLocation = entry.getKey();
						String imageUrl = entry.getValue();

						// 4. 파일 저장 - transferTo는 내부적으로 non-blocking
						return filePart.transferTo(targetLocation)
							.then(Mono.just(ImageUrlDTO.builder().url(imageUrl).build()))
							.onErrorResume(e -> {
								log.error("이미지 저장 중 오류 발생", e);
								return Mono.error(new RuntimeException("이미지 저장 중 오류가 발생했습니다."));
							});
					});
			});
	}

	// 파일 유효성 검사 - 이 메서드도 리액티브하게 수정
	private Mono<Boolean> validateFile(FilePart filePart) {
		if (filePart == null) {
			return Mono.error(new RuntimeException("파일이 없습니다."));
		}

		String originalFilename = filePart.filename();
		String extension = getFileExtension(originalFilename).toLowerCase();

		// 허용된 확장자 검사
		if (!ALLOWED_EXTENSIONS.contains(extension)) {
			return Mono.error(new RuntimeException(
				"허용되지 않는 파일 형식입니다. 허용된 형식: " + String.join(", ", ALLOWED_EXTENSIONS)));
		}

		// 파일 크기 검사
		return DataBufferUtils.join(filePart.content())
			.handle((dataBuffer, sink) -> {
				try {
					long fileSize = dataBuffer.readableByteCount();
					if (fileSize > MAX_FILE_SIZE) {
						sink.error(new RuntimeException(
							"파일 크기가 너무 큽니다. 최대 허용 크기: " + (MAX_FILE_SIZE / (1024 * 1024)) + "MB"));
						return;
					}
					sink.next(true);
				} finally {
					DataBufferUtils.release(dataBuffer);  // 중요: 리소스 해제
				}
			});
	}

	// 파일 확장자 추출
	private String getFileExtension(String filename) {
		return Optional.ofNullable(filename)
			.filter(f -> f.contains("."))
			.map(f -> f.substring(filename.lastIndexOf(".") + 1))
			.orElse("");
	}

	// 고유한 파일명 생성
	private String generateUniqueFilename(String extension) {
		String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		String random = UUID.randomUUID().toString().substring(0, 8);
		return timestamp + "_" + random + "." + extension;
	}
}
