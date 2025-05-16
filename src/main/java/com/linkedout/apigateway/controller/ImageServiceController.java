package com.linkedout.apigateway.controller;

import com.linkedout.apigateway.service.ImageService;
import com.linkedout.common.model.dto.BaseApiResponse;
import com.linkedout.common.model.dto.ImageUrlDTO;
import com.linkedout.common.model.type.ImageType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;


@Slf4j
@Tag(name = "Image", description = "이미지 서비스 앤드포인트")
@RestController
@RequestMapping("/api/images")
public class ImageServiceController {

	private final ImageService imageService;

	public ImageServiceController(ImageService imageService) {
		this.imageService = imageService;
	}

	@Operation(
		summary = "이미지 업로드",
		description = """
			지정된 타입의 이미지를 업로드합니다.
			
			지원되는 이미지 경로 파라미터:
			- recipe/main: 레시피 대표 이미지
			- recipe/step: 레시피 단계별 이미지
			- profile: 프로필 이미지
			- general: 일반 이미지
			""",
		responses = {
			@ApiResponse(
				responseCode = "201",
				description = "이미지 업로드 성공",
				content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ImageUrlDTO.class)
				)
			),
			@ApiResponse(
				responseCode = "400",
				description = "잘못된 이미지 타입",
				content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = BaseApiResponse.class)
				)
			)
		}
	)
	@PostMapping({"{imageType}", "/recipe/{imageType}"})
	public Mono<ResponseEntity<BaseApiResponse<?>>> uploadGenericImage(
		@Parameter(name = "file", schema = @Schema(implementation = FilePart.class), required = true)
		@RequestPart("file") FilePart filePart,
		@Parameter(name = "imageType", schema = @Schema(implementation = String.class), required = true)
		@PathVariable String imageType) {
		ImageType type;
		try {
			type = ImageType.fromString(imageType);
		} catch (IllegalArgumentException e) {
			return Mono.just(ResponseEntity
				.badRequest()
				.body(BaseApiResponse.error(400, e.getMessage(), "지원하지 않는 이미지 타입입니다.")));
		}

		return imageService.storeImage(filePart, type)
			.map(imageUrl -> ResponseEntity.status(201).body(BaseApiResponse.success(201, imageUrl, "이미지가 성공적으로 업로드되었습니다.")));
	}
}


