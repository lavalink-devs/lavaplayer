package com.sedmelluq.discord.lavaplayer.track;

import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;

/**
 * Audio track which delegates its processing to another track. The delegate does not have to be known when the
 * track is created, but is passed when processDelegate() is called.
 */
public abstract class DelegatedAudioTrack extends BaseAudioTrack {
    private InternalAudioTrack delegate;

    /**
     * @param trackInfo Track info
     */
    public DelegatedAudioTrack(AudioTrackInfo trackInfo) {
        super(trackInfo);
    }

    protected synchronized void processDelegate(InternalAudioTrack delegate, LocalAudioTrackExecutor localExecutor)
        throws Exception {

        this.delegate = delegate;

        delegate.assignExecutor(localExecutor, false);
        delegate.process(localExecutor);
    }

    @Override
    public void setPosition(long position) {
        if (delegate != null) {
            delegate.setPosition(position);
        } else {
            synchronized (this) {
                if (delegate != null) {
                    delegate.setPosition(position);
                } else {
                    super.setPosition(position);
                }
            }
        }
    }

    @Override
    public long getDuration() {
        if (delegate != null) {
            return delegate.getDuration();
        } else {
            synchronized (this) {
                if (delegate != null) {
                    return delegate.getDuration();
                } else {
                    return super.getDuration();
                }
            }
        }
    }

    @Override
    public long getPosition() {
        if (delegate != null) {
            return delegate.getPosition();
        } else {
            synchronized (this) {
                if (delegate != null) {
                    return delegate.getPosition();
                } else {
                    return super.getPosition();
                }
            }
        }
    }
}
