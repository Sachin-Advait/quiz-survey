package com.gissoftware.quiz_survey.mapper;

import com.gissoftware.quiz_survey.dto.TrainingEditDTO;
import com.gissoftware.quiz_survey.model.TrainingAssignment;
import com.gissoftware.quiz_survey.model.TrainingMaterial;
import java.util.List;

public class TrainingMapper {

  private TrainingMapper() {}

  public static TrainingEditDTO toEditDTO(
      TrainingMaterial material, List<TrainingAssignment> assignments) {

    return TrainingEditDTO.builder()
        .id(material.getId())
        .title(material.getTitle())
        .type(material.getType())
        .duration(material.getDuration())
        .durationSeconds(material.getDurationSeconds())
        .region(material.getRegion())
        .assignedTo(material.getAssignedTo())
        .completionRate(material.getCompletionRate())
        .dueDate(material.getDueDate())

        // ✅ VIDEO (GENERIC)
        .videoProvider(material.getVideoProvider())
        .videoPublicId(material.getVideoPublicId())
        .videoPlaybackUrl(material.getVideoPlaybackUrl())
        .videoFormat(material.getVideoFormat())
        .active(material.getActive())
        .uploadDate(material.getUploadDate())
        .assignedUserIds(assignments.stream().map(TrainingAssignment::getUserId).toList())
        .build();
  }
}
