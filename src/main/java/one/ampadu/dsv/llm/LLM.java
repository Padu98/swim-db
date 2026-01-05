package one.ampadu.dsv.llm;

public interface LLM {

    ExecutionResult execute(String prompt);
    Provider getProvider();
    boolean isFree();

    class LoadLLMException extends RuntimeException {
        public LoadLLMException(String errorMessage) {
            super(errorMessage);
        }
    }

    enum Provider{
        Gemini,
        EdenAiPaid
    }

    sealed interface ExecutionResult permits Success, Error, ChangedModel {}
    record Success(String data) implements ExecutionResult {}
    record Error() implements ExecutionResult {}
    record ChangedModel(String name) implements ExecutionResult {}
}
