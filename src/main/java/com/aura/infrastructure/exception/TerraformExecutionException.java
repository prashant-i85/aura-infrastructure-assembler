package com.aura.infrastructure.exception;

public class TerraformExecutionException extends RuntimeException {

    private final String terraformOutput;

    public TerraformExecutionException(String message) {
        super(message);
        this.terraformOutput = null;
    }

    public TerraformExecutionException(String message, String terraformOutput) {
        super(message);
        this.terraformOutput = terraformOutput;
    }

    public TerraformExecutionException(String message, Throwable cause) {
        super(message, cause);
        this.terraformOutput = null;
    }

    public String getTerraformOutput() {
        return terraformOutput;
    }
}
