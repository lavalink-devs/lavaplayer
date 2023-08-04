package com.sedmelluq.discord.lavaplayer.tools;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class FutureTools {
    private static final Logger log = LoggerFactory.getLogger(FutureTools.class);

    @SuppressWarnings("unchecked")
    public static <T> List<T> awaitList(CompletionService<T> completionService, List<Future<AudioTrack>> futures) {

        int received = 0;
        boolean failed = false;
        while (received < futures.size() && !failed) {
            try {
                completionService.take();
                received++;
            } catch (InterruptedException e) {
                log.debug("Received an interruption while taking item ", e);
                failed = true;
            } catch (Exception e) {
                log.debug("Some error occurred while getting futures", e);
                failed = true;
            }
        }

        try {
            return (List<T>) futures.stream()
                .filter(Future::isDone)
                .map(e -> {
                    try {
                        return e.get();
                    } catch (Exception ex) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.debug("Some error occurred while getting futures", e);
            return Collections.emptyList();
        }
    }
}
