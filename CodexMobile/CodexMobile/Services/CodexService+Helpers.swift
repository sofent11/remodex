// FILE: CodexService+Helpers.swift
// Purpose: Shared utility helpers for model decoding and thread bookkeeping.
// Layer: Service
// Exports: CodexService helpers
// Depends on: Foundation

import Foundation

extension CodexService {
    func resolveThreadID(_ preferredThreadID: String?) async throws -> String {
        if let preferredThreadID, !preferredThreadID.isEmpty {
            return preferredThreadID
        }

        if let activeThreadId, !activeThreadId.isEmpty {
            return activeThreadId
        }

        let newThread = try await startThread()
        return newThread.id
    }

    func upsertThread(_ thread: CodexThread) {
        if let existingIndex = threads.firstIndex(where: { $0.id == thread.id }) {
            threads[existingIndex] = thread
        } else {
            threads.append(thread)
        }

        threads = sortThreads(threads)
    }

    func sortThreads(_ value: [CodexThread]) -> [CodexThread] {
        value.sorted { lhs, rhs in
            let lhsDate = lhs.updatedAt ?? lhs.createdAt ?? Date.distantPast
            let rhsDate = rhs.updatedAt ?? rhs.createdAt ?? Date.distantPast
            return lhsDate > rhsDate
        }
    }

    func decodeModel<T: Decodable>(_ type: T.Type, from value: JSONValue) -> T? {
        guard let data = try? encoder.encode(value) else {
            return nil
        }

        return try? decoder.decode(type, from: data)
    }

    func rememberSuccessfulTransportURL(_ url: String) {
        let normalized = url.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalized.isEmpty else {
            return
        }
        lastSuccessfulTransportURL = normalized
        SecureStore.writeString(normalized, for: CodexSecureKeys.pairingLastSuccessfulTransportURL)
    }

    func setPreferredTransportURL(_ url: String?) {
        let trimmed = url?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let normalized = trimmed.isEmpty ? nil : trimmed
        preferredTransportURL = normalized
        if let normalized {
            SecureStore.writeString(normalized, for: CodexSecureKeys.pairingPreferredTransportURL)
        } else {
            SecureStore.deleteValue(for: CodexSecureKeys.pairingPreferredTransportURL)
        }
    }

    func displayTitle(for candidate: CodexTransportCandidate) -> String {
        if let label = candidate.label, !label.isEmpty {
            return label
        }

        switch candidate.kind {
        case "local_ipv4":
            return "Local Network"
        case "local_hostname":
            return "Local Hostname"
        case "tailnet_ipv4", "tailnet":
            return "Tailscale"
        default:
            return candidate.kind.replacingOccurrences(of: "_", with: " ").capitalized
        }
    }

    func extractTurnID(from value: JSONValue?) -> String? {
        guard let object = value?.objectValue else {
            return nil
        }

        if let turnId = object["turn"]?.objectValue?["id"]?.stringValue {
            return turnId
        }
        if let turnId = object["turnId"]?.stringValue {
            return turnId
        }
        if let turnId = object["turn_id"]?.stringValue {
            return turnId
        }

        guard let fallbackId = object["id"]?.stringValue else {
            return nil
        }

        // Avoid misclassifying item payload ids as turn ids.
        let looksLikeItemPayload = object["type"] != nil
            || object["item"] != nil
            || object["content"] != nil
            || object["output"] != nil
        if looksLikeItemPayload {
            return nil
        }

        return fallbackId
    }

}
