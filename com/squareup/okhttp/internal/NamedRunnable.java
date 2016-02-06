package com.squareup.okhttp.internal;

public abstract class NamedRunnable implements Runnable {
    private String name;

    protected abstract void execute();

    public NamedRunnable(String name) {
        this.name = name;
    }

    public final void run() {
        String oldName = Thread.currentThread().getName();
        Thread.currentThread().setName(this.name);
        try {
            execute();
        } finally {
            Thread.currentThread().setName(oldName);
        }
    }
}
