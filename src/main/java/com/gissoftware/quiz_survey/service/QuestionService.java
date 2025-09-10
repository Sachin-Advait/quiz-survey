package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.model.QuestionModel;
import com.gissoftware.quiz_survey.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;

    // Import bulk questions (global pool)
    public List<QuestionModel> importQuestions(List<QuestionModel> questions) {
        return questionRepository.saveAll(questions);
    }
}
